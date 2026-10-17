/**
 * Dispatcher Console: Login and Phone Intake Logging
 */
import { state, setDispatcherSession } from '../state.js';
import { api } from '../api.js';
import { showAlert } from '../ui.js';

let dispatchAssignmentList;
let availableDrivers = [];

function updateDispatcherAuthUI() {
    const authBadge = document.getElementById('dispatcherAuthBadge');
    const logoutBtn = document.getElementById('btnDispatcherLogout');
    const loginBox = document.getElementById('dispatcherLoginBox');

    if (state.dispatcherToken) {
        const roleLabel = state.dispatcherRole ? state.dispatcherRole.replace(/^ROLE_/, '').replace(/_/g, ' ') : 'Dispatcher';
        authBadge.textContent = `Logged in as: ${state.dispatcherName || 'Dispatcher'} (${roleLabel})`;
        logoutBtn.classList.remove('hidden');
        loginBox.classList.add('hidden');
    } else {
        authBadge.textContent = 'Not Logged In';
        logoutBtn.classList.add('hidden');
        loginBox.classList.remove('hidden');
        if (dispatchAssignmentList) {
            dispatchAssignmentList.innerHTML = '<p class="text-sm text-slate-500 italic">Log in to view open dispatch cases and assign drivers.</p>';
        }
    }
}

function renderDispatchQueue(bookings) {
    if (!dispatchAssignmentList) return;

    if (!bookings.length) {
        dispatchAssignmentList.innerHTML = '<p class="text-sm text-slate-500 italic">No open requested bookings right now.</p>';
        return;
    }

    const driverOptions = availableDrivers.length
        ? availableDrivers.map(driver => `
            <option value="${driver.id}">${driver.fullName} (${driver.phone})</option>
        `).join('')
        : '<option value="">No available drivers</option>';

    dispatchAssignmentList.innerHTML = '';

    bookings.forEach(booking => {
        const card = document.createElement('div');
        card.className = 'bg-slate-50 border rounded-lg p-4 space-y-3';
        const assignedLabel = booking.driverName
            ? `${booking.driverName} (${booking.driverPhone || 'no phone'})`
            : 'Unassigned';
        const buttonDisabled = availableDrivers.length === 0 ? 'disabled' : '';

        card.innerHTML = `
            <div class="flex flex-wrap justify-between gap-3 items-start">
                <div>
                    <div class="font-bold text-slate-800">#${booking.id} - ${booking.contactName}</div>
                    <div class="text-xs text-slate-600 mt-1">Pickup: ${booking.pickupAddress}</div>
                    <div class="text-xs text-slate-500">Condition: ${booking.patientCondition || 'Emergency'}</div>
                </div>
                <div class="text-right text-xs space-y-1">
                    <span class="inline-block px-2 py-0.5 rounded-full bg-yellow-100 text-yellow-800 font-semibold">${booking.status}</span>
                    <div class="text-slate-600">Assigned driver: <span class="font-semibold">${assignedLabel}</span></div>
                </div>
            </div>
            <div class="flex flex-wrap gap-2 items-center">
                <select data-booking-id="${booking.id}" class="dispatch-driver-select border rounded px-3 py-2 text-sm min-w-[240px] flex-1">
                    <option value="">Select available driver</option>
                    ${driverOptions}
                </select>
                <button type="button" data-booking-id="${booking.id}" class="dispatch-assign-btn bg-amber-600 hover:bg-amber-700 disabled:bg-slate-300 disabled:text-slate-500 text-white font-semibold px-4 py-2 rounded text-sm transition" ${buttonDisabled}>
                    ${booking.driverId ? 'Reassign Driver' : 'Assign Driver'}
                </button>
                ${booking.driverId ? `
                    <button type="button" data-booking-id="${booking.id}" class="dispatch-unassign-btn bg-slate-600 hover:bg-slate-700 text-white font-semibold px-4 py-2 rounded text-sm transition">
                        Unassign
                    </button>
                ` : ''}
            </div>
        `;

        const select = card.querySelector('.dispatch-driver-select');
        if (booking.driverId) {
            select.value = String(booking.driverId);
        }

        dispatchAssignmentList.appendChild(card);
    });
}

async function refreshDispatchAssignmentPanel() {
    if (!state.dispatcherToken) return;

    try {
        const [bookings, drivers] = await Promise.all([
            api.dispatch.getOpenBookings(),
            api.dispatch.getAvailableDrivers()
        ]);
        availableDrivers = drivers;
        renderDispatchQueue(bookings);
    } catch (err) {
        showAlert('Failed to load dispatch assignment data: ' + err.message, 'error');
    }
}

async function handleAssignDriverClick(button) {
    const bookingId = button.dataset.bookingId;
    const select = dispatchAssignmentList?.querySelector(`.dispatch-driver-select[data-booking-id="${bookingId}"]`);
    const driverId = select?.value;

    if (!driverId) {
        showAlert('Please choose an available driver first.', 'error');
        return;
    }

    try {
        const updated = await api.dispatch.assignDriver(bookingId, Number(driverId));
        const driverLabel = updated.driverName ? `${updated.driverName}` : `driver #${driverId}`;
        showAlert(`Booking #${bookingId} reserved for ${driverLabel}. The driver must still accept the trip.`, 'success');
        await refreshDispatchAssignmentPanel();
    } catch (err) {
        showAlert('Driver assignment failed: ' + err.message, 'error');
    }
}

async function handleUnassignDriverClick(button) {
    const bookingId = button.dataset.bookingId;

    try {
        await api.dispatch.unassignDriver(bookingId);
        showAlert(`Booking #${bookingId} is now back in the open queue.`, 'info');
        await refreshDispatchAssignmentPanel();
    } catch (err) {
        showAlert('Driver unassignment failed: ' + err.message, 'error');
    }
}

async function handlePhoneDispatchSubmit(e) {
    e.preventDefault();
    if (!state.dispatcherToken) return showAlert('Please log in as Dispatcher first.', 'error');

    const payload = {
        callerName: document.getElementById('callContactName').value,
        callerPhone: document.getElementById('callContactPhone').value,
        pickupAddress: document.getElementById('callPickupAddress').value,
        pickupLatitude: 12.9716,
        pickupLongitude: 77.5946,
        destinationAddress: 'Nearest Trauma Center',
        destinationLatitude: 12.9780,
        destinationLongitude: 77.6000,
        ambulanceType: 'ADVANCED_LIFE_SUPPORT',
        triageNotes: document.getElementById('callNotes').value.trim()
    };

    try {
        const data = await api.dispatch.logPhoneCall(payload);
        const recorderLabel = data.recordedByName
            ? `${data.recordedByName}${data.recordedByRole ? ` (${data.recordedByRole.replace(/^ROLE_/, '').replace(/_/g, ' ')})` : ''}`
            : 'staff console';
        showAlert(`Call Intake Recorded! Booking #${data.id} created and logged by ${recorderLabel}.`, 'success');
        document.getElementById('phoneDispatchForm').reset();
        await refreshDispatchAssignmentPanel();
    } catch (err) {
        showAlert('Dispatch call failed: ' + err.message, 'error');
    }
}

export function initDispatcherModule() {
    dispatchAssignmentList = document.getElementById('dispatchAssignmentList');
    updateDispatcherAuthUI();

    document.getElementById('btnDispatcherLogin')?.addEventListener('click', async () => {
        const email = document.getElementById('dispatcherEmail').value;
        const password = document.getElementById('dispatcherPassword').value;
        try {
            const data = await api.auth.login(email, password);
            if (!['ROLE_DISPATCHER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN'].includes(data.role)) {
                showAlert('You are not authorized to access the dispatcher console.', 'error');
                return;
            }
            setDispatcherSession(data.token, data.fullName, data.role);
            updateDispatcherAuthUI();
            showAlert('Dispatcher logged in!', 'success');
            refreshDispatchAssignmentPanel();
        } catch (err) {
            showAlert(`Login failed: ${err.message}`, 'error');
        }
    });

    document.getElementById('btnDispatcherLogout')?.addEventListener('click', () => {
        setDispatcherSession(null, null, null);
        availableDrivers = [];
        updateDispatcherAuthUI();
        showAlert('Dispatcher logged out.', 'info');
    });

    document.getElementById('phoneDispatchForm')?.addEventListener('submit', handlePhoneDispatchSubmit);
    document.getElementById('btnRefreshDispatchAssignments')?.addEventListener('click', refreshDispatchAssignmentPanel);
    dispatchAssignmentList?.addEventListener('click', (e) => {
        if (e.target && e.target.classList.contains('dispatch-assign-btn')) {
            handleAssignDriverClick(e.target);
        }
        if (e.target && e.target.classList.contains('dispatch-unassign-btn')) {
            handleUnassignDriverClick(e.target);
        }
    });

    if (state.dispatcherToken) {
        refreshDispatchAssignmentPanel();
    }
}
