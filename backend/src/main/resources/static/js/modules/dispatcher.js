/**
 * Dispatcher Console: Login and Phone Intake Logging
 */
import { state, setDispatcherSession } from '../state.js';
import { api } from '../api.js';
import { showAlert } from '../ui.js';

let dispatchAssignmentList;
let availableDrivers = [];
let driverCandidates = [];

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
            <option value="${driver.id}">${driver.fullName} (${driver.phone})${driver.ambulanceRegistrationNumber ? ` • ${driver.ambulanceRegistrationNumber}` : ''}</option>
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
                <div class="flex-1 min-w-0">
                    <div class="font-bold text-slate-800">#${booking.id} - ${booking.contactName}</div>
                    <div class="text-xs text-slate-600 mt-1"><span class="font-semibold">Pickup:</span> ${booking.pickupAddress || 'No pickup address'}</div>
                    <div class="text-xs text-slate-600"><span class="font-semibold">Caller:</span> <a href="tel:${booking.contactPhone || ''}" class="text-blue-700 hover:underline">${booking.contactPhone || 'No phone available'}</a></div>
                    <div class="text-xs text-slate-500 mt-1">Destination: ${booking.destinationAddress || 'Not provided'}</div>
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
        syncBookingSelectorWithQueue(bookings);
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

async function setNearestDriverReferenceFromBooking(booking) {
    const latInput = document.getElementById('dispatchPickupLatitude');
    const lonInput = document.getElementById('dispatchPickupLongitude');
    if (!latInput || !lonInput || !booking) return;

    const pickupLatitude = booking.pickupLatitude ?? null;
    const pickupLongitude = booking.pickupLongitude ?? null;

    latInput.value = pickupLatitude ?? '';
    lonInput.value = pickupLongitude ?? '';
    latInput.readOnly = true;
    lonInput.readOnly = true;
    latInput.classList.add('bg-slate-100');
    lonInput.classList.add('bg-slate-100');
}

async function refreshNearestDriversForSelectedBooking() {
    const bookingId = document.getElementById('nearestDriverBookingId')?.value;
    if (!bookingId) {
        const candidateContainer = document.getElementById('dispatchNearestDrivers');
        if (candidateContainer) {
            candidateContainer.innerHTML = '<p class="text-sm text-slate-500 italic">Select a booking to rank nearby drivers.</p>';
        }
        return;
    }

    try {
        const bookings = await api.dispatch.getOpenBookings();
        const selectedBooking = bookings.find(booking => String(booking.id) === String(bookingId));
        if (!selectedBooking) {
            throw new Error('Selected booking not found.');
        }

        setNearestDriverReferenceFromBooking(selectedBooking);

        const pickupLatitude = Number(selectedBooking.pickupLatitude ?? 0);
        const pickupLongitude = Number(selectedBooking.pickupLongitude ?? 0);

        if (!pickupLatitude || !pickupLongitude) {
            const candidateContainer = document.getElementById('dispatchNearestDrivers');
            if (candidateContainer) {
                candidateContainer.innerHTML = '<p class="text-sm text-slate-500 italic">Selected booking has no valid pickup coordinates.</p>';
            }
            return;
        }

        driverCandidates = await api.dispatch.getNearestDrivers(
            pickupLatitude,
            pickupLongitude,
            pickupLatitude,
            pickupLongitude
        );
        renderNearestDriverCandidates(driverCandidates);
    } catch (err) {
        showAlert('Failed to load nearest available drivers for the selected booking: ' + err.message, 'error');
    }
}

async function refreshNearestDrivers() {
    if (!state.dispatcherToken) return;

    const bookingId = document.getElementById('nearestDriverBookingId')?.value;
    if (bookingId) {
        await refreshNearestDriversForSelectedBooking();
        return;
    }

    const pickupLatitude = Number(document.getElementById('dispatchPickupLatitude')?.value || 0);
    const pickupLongitude = Number(document.getElementById('dispatchPickupLongitude')?.value || 0);

    if (!pickupLatitude || !pickupLongitude) {
        const candidateContainer = document.getElementById('dispatchNearestDrivers');
        if (candidateContainer) {
            candidateContainer.innerHTML = '<p class="text-sm text-slate-500 italic">Select a booking to rank nearby drivers.</p>';
        }
        return;
    }

    try {
        driverCandidates = await api.dispatch.getNearestDrivers(pickupLatitude, pickupLongitude, pickupLatitude, pickupLongitude);
        renderNearestDriverCandidates(driverCandidates);
    } catch (err) {
        showAlert('Failed to load nearest available drivers: ' + err.message, 'error');
    }
}

function renderNearestDriverCandidates(candidates) {
    const candidateContainer = document.getElementById('dispatchNearestDrivers');
    if (!candidateContainer) return;

    if (!candidates.length) {
        candidateContainer.innerHTML = '<p class="text-sm text-slate-500 italic">No nearby available drivers yet.</p>';
        return;
    }

    candidateContainer.innerHTML = candidates.map((driver, index) => `
        <div class="border border-slate-200 rounded-lg p-3 bg-slate-50">
            <div class="flex items-center justify-between gap-3 mb-2">
                <div class="flex items-center gap-2">
                    <span class="inline-flex items-center justify-center w-6 h-6 rounded-full bg-amber-100 text-amber-700 text-xs font-bold">${index + 1}</span>
                    <strong class="text-slate-800">${driver.fullName}</strong>
                </div>
                <span class="text-xs font-semibold px-2 py-1 rounded-full bg-green-100 text-green-700">${driver.availabilityStatus}</span>
            </div>
            <div class="text-xs text-slate-600 space-y-1">
                <div><span class="font-medium">Distance:</span> ${Number(driver.distanceKm || 0).toFixed(1)} km</div>
                <div><span class="font-medium">Phone:</span> ${driver.phone || 'N/A'}</div>
                <div><span class="font-medium">Ambulance:</span> ${driver.ambulanceRegistrationNumber || 'N/A'}</div>
                <div><span class="font-medium">Last seen:</span> ${driver.lastLocationUpdate ? new Date(driver.lastLocationUpdate).toLocaleString() : 'N/A'}</div>
                <div><span class="font-medium">Address:</span> ${driver.address || 'N/A'}</div>
            </div>
            <div class="mt-3 flex gap-2">
                <button type="button" data-driver-id="${driver.id}" class="dispatch-select-nearest-driver-btn bg-amber-600 hover:bg-amber-700 text-white px-3 py-1.5 rounded text-xs font-semibold transition">
                    Select Driver
                </button>
            </div>
        </div>
    `).join('');
}

async function handleSelectNearestDriverClick(button) {
    const driverId = Number(button.dataset.driverId);
    const bookingId = document.getElementById('nearestDriverBookingId')?.value;

    if (!bookingId) {
        showAlert('Please select or open a booking before assigning a ranked driver.', 'info');
        return;
    }

    try {
        const updated = await api.dispatch.assignDriver(Number(bookingId), driverId);
        showAlert(`Booking #${bookingId} reserved for ${updated.driverName || 'selected driver'}.`, 'success');
        await refreshDispatchAssignmentPanel();
        await refreshNearestDrivers();
    } catch (err) {
        showAlert('Failed to assign the selected nearby driver: ' + err.message, 'error');
    }
}

function syncBookingSelectorWithQueue(bookings) {
    const bookingSelect = document.getElementById('nearestDriverBookingId');
    if (!bookingSelect) return;

    const currentValue = bookingSelect.value;
    bookingSelect.innerHTML = '<option value="">Select a booking</option>' + bookings.map(booking => `
        <option value="${booking.id}">${booking.id} - ${booking.contactName}</option>
    `).join('');

    if (currentValue && bookings.some(booking => String(booking.id) === String(currentValue))) {
        bookingSelect.value = currentValue;
    }

    const selectedBooking = bookings.find(booking => String(booking.id) === String(bookingSelect.value));
    const latInput = document.getElementById('dispatchPickupLatitude');
    const lonInput = document.getElementById('dispatchPickupLongitude');

    if (selectedBooking) {
        setNearestDriverReferenceFromBooking(selectedBooking);
    } else if (latInput && lonInput) {
        latInput.readOnly = true;
        lonInput.readOnly = true;
        latInput.classList.add('bg-slate-100');
        lonInput.classList.add('bg-slate-100');
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
    document.getElementById('btnRefreshNearestDrivers')?.addEventListener('click', refreshNearestDrivers);
    document.getElementById('nearestDriverBookingId')?.addEventListener('change', refreshNearestDriversForSelectedBooking);
    dispatchAssignmentList?.addEventListener('click', (e) => {
        if (e.target && e.target.classList.contains('dispatch-assign-btn')) {
            handleAssignDriverClick(e.target);
        }
        if (e.target && e.target.classList.contains('dispatch-unassign-btn')) {
            handleUnassignDriverClick(e.target);
        }
    });

    document.getElementById('dispatchNearestDrivers')?.addEventListener('click', (e) => {
        if (e.target && e.target.classList.contains('dispatch-select-nearest-driver-btn')) {
            handleSelectNearestDriverClick(e.target);
        }
    });

    if (state.dispatcherToken) {
        refreshDispatchAssignmentPanel();
    }
}
