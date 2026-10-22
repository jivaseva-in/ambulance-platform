/**
 * Driver Operations: Login, Active Trip Lifecycle, Job Queue, and GPS Simulation
 */
import { state, setDriverSession, setDriverActiveTrip } from '../state.js';
import { api } from '../api.js';
import { showAlert } from '../ui.js';

export function initDriverModule() {
    updateDriverAuthUI();

    document.getElementById('btnDriverLogin')?.addEventListener('click', handleDriverLogin);
    document.getElementById('btnDriverLogout')?.addEventListener('click', handleDriverLogout);
    document.getElementById('btnRefreshJobs')?.addEventListener('click', loadAvailableJobs);
    document.getElementById('btnSyncActiveTrip')?.addEventListener('click', syncActiveTripFromBackend);
    document.getElementById('btnToggleAvailability')?.addEventListener('click', handleToggleAvailability);
    document.getElementById('btnRefreshDriverLocation')?.addEventListener('click', handleRefreshLocation);

    // Lifecycle transition buttons
    document.getElementById('btnSetInTransit')?.addEventListener('click', () => handleStatusChange('IN_TRANSIT'));
    document.getElementById('btnSetArrived')?.addEventListener('click', () => handleStatusChange('ARRIVED'));
    document.getElementById('btnSetCompleted')?.addEventListener('click', () => handleStatusChange('COMPLETED'));
    document.getElementById('btnSetCancelled')?.addEventListener('click', () => handleStatusChange('CANCELLED'));
    document.getElementById('btnPushGpsTelemetry')?.addEventListener('click', handlePushGps);

    if (state.driverToken) {
        loadDriverAvailability();
        if (state.driverActiveTrip) {
            updateDriverStatusCard(state.driverActiveTrip);
        }
    }
}

export function updateDriverAuthUI() {
    const authBadge = document.getElementById('driverAuthBadge');
    const logoutBtn = document.getElementById('btnDriverLogout');
    const loginBox = document.getElementById('driverLoginBox');

    if (state.driverToken) {
        authBadge.textContent = `Logged in as: ${state.driverName || 'Driver'}`;
        logoutBtn.classList.remove('hidden');
        loginBox.classList.add('hidden');
        loadDriverAvailability();
    } else {
        authBadge.textContent = 'Not Logged In';
        logoutBtn.classList.add('hidden');
        loginBox.classList.remove('hidden');
        driverAvailability = null;
        renderDriverAvailability({ availabilityStatus: 'UNAVAILABLE', latitude: null, longitude: null });
        document.getElementById('availableJobsList').innerHTML = '<p class="text-sm text-slate-500 italic">Please log in to view pending emergency requests.</p>';
    }
}

export function updateDriverStatusCard(booking) {
    const targetIdInput = document.getElementById('driverTargetBookingId');
    const statusBadge = document.getElementById('driverTargetStatusBadge');
    const summaryDiv = document.getElementById('driverTargetPatientSummary');

    if (!booking) {
        statusBadge.innerHTML = '<span class="text-slate-400">-- No Trip Loaded --</span>';
        summaryDiv.textContent = '--';
        return;
    }

    targetIdInput.value = booking.id;
    statusBadge.innerHTML = `<span class="px-2 py-0.5 rounded text-xs font-bold ${
        booking.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-800' :
            booking.status === 'ARRIVED' ? 'bg-indigo-100 text-indigo-800' :
                booking.status === 'IN_TRANSIT' ? 'bg-amber-100 text-amber-800' :
                    booking.status === 'ASSIGNED' ? 'bg-blue-100 text-blue-800' :
                        'bg-slate-100 text-slate-800'
    }">${booking.status}</span>`;
    summaryDiv.textContent = `${booking.contactName || 'Emergency'} (${booking.pickupAddress || 'No address'})`;
}

export async function syncActiveTripFromBackend() {
    if (!state.driverToken) return;
    try {
        const activeTrip = await api.bookings.getActiveForDriver();
        if (activeTrip) {
            setDriverActiveTrip(activeTrip);
            updateDriverStatusCard(activeTrip);
        } else if (state.driverActiveTrip) {
            updateDriverStatusCard(state.driverActiveTrip);
        }
    } catch (err) {
        console.error('Error syncing active trip:', err);
    }
}

async function handleDriverLogin() {
    const email = document.getElementById('driverEmail').value;
    const password = document.getElementById('driverPassword').value;

    try {
        const data = await api.auth.login(email, password);
        setDriverSession(data.token, data.fullName);
        updateDriverAuthUI();
        showAlert('Driver logged in successfully!', 'success');
        loadAvailableJobs();
        syncActiveTripFromBackend();
    } catch (err) {
        showAlert('Driver login failed: ' + err.message, 'error');
    }
}

function handleDriverLogout() {
    setDriverSession(null, null);
    updateDriverAuthUI();
    updateDriverStatusCard(null);
    showAlert('Driver logged out.', 'info');
}

export async function loadAvailableJobs() {
    if (!state.driverToken) return;
    try {
        const jobs = await api.bookings.getAvailable();
        const container = document.getElementById('availableJobsList');
        container.innerHTML = '';

        if (jobs.length === 0) {
            container.innerHTML = '<p class="text-sm text-slate-500 italic">No available emergency requests right now.</p>';
            return;
        }

        jobs.forEach(job => {
            const assignedToMe = Boolean(job.driverId);
            const card = document.createElement('div');
            card.className = 'p-4 bg-slate-50 border rounded-lg flex justify-between items-center';
            card.innerHTML = `
                <div>
                  <div class="font-bold text-slate-800">#${job.id} - ${job.contactName} (${job.ambulanceType})</div>
                  <div class="text-xs text-slate-600">Pickup: ${job.pickupAddress}</div>
                  <div class="text-xs text-slate-500">Condition: ${job.patientCondition || 'Emergency'}</div>
                  <div class="text-xs mt-1 ${assignedToMe ? 'text-blue-700 font-semibold' : 'text-slate-400'}">${assignedToMe ? 'Reserved for you by dispatch/admin' : 'Open queue - any available driver may accept'}</div>
                </div>
                <button data-accept-id="${job.id}" class="btn-accept bg-emerald-600 hover:bg-emerald-700 text-white font-bold px-3 py-1.5 rounded text-xs transition">
                  ${assignedToMe ? 'Accept Assigned Trip' : 'Accept Trip'}
                </button>
            `;
            container.appendChild(card);
        });

        container.querySelectorAll('.btn-accept').forEach(btn => {
            btn.addEventListener('click', () => acceptJob(btn.dataset.acceptId));
        });
    } catch (err) {
        showAlert('Error loading jobs: ' + err.message, 'error');
    }
}

async function acceptJob(bookingId) {
    try {
        const updated = await api.bookings.accept(bookingId);
        setDriverActiveTrip(updated);
        showAlert(`You accepted booking #${bookingId}! Status: ASSIGNED.`, 'success');
        updateDriverStatusCard(updated);
        loadAvailableJobs();
    } catch (err) {
        showAlert('Failed to accept trip: ' + err.message, 'error');
    }
}

async function handleStatusChange(newStatus) {
    const bookingId = document.getElementById('driverTargetBookingId').value;
    if (!bookingId) return showAlert('Please enter or select a Booking ID first!', 'error');
    if (!state.driverToken) return showAlert('Please log in as Driver first!', 'error');

    try {
        const updated = await api.bookings.updateStatus(bookingId, newStatus);
        showAlert(`Trip #${bookingId} status updated to: ${newStatus}`, 'success');

        if (newStatus === 'COMPLETED' || newStatus === 'CANCELLED') {
            setDriverActiveTrip(null);
        } else {
            setDriverActiveTrip(updated);
        }
        updateDriverStatusCard(updated);
        loadAvailableJobs();
    } catch (err) {
        showAlert('Failed to update status: ' + err.message, 'error');
    }
}

async function handlePushGps() {
    const bookingId = document.getElementById('driverTargetBookingId').value;
    if (!bookingId) return showAlert('Please enter or select a Booking ID first!', 'error');

    const lat = 12.9716 + (Math.random() - 0.5) * 0.01;
    const lng = 77.5946 + (Math.random() - 0.5) * 0.01;
    const speed = 40 + Math.random() * 25;

    try {
        await api.bookings.pushLocation(bookingId, lat, lng, speed);
        showAlert(`GPS Telemetry broadcasted (${speed.toFixed(1)} km/h) for Booking #${bookingId}`, 'info');
    } catch (err) {
        console.error('Failed to send GPS telemetry:', err);
    }
}

let driverAvailability = null;

async function loadDriverAvailability() {
    if (!state.driverToken) return;
    try {
        driverAvailability = await api.drivers.getAvailability();
        renderDriverAvailability(driverAvailability);
    } catch (err) {
        console.error('Failed to load driver availability:', err);
    }
}

function renderDriverAvailability(data) {
    const statusBadge = document.getElementById('driverAvailabilityBadge');
    const locationOut = document.getElementById('driverLocationSummary');
    const toggleBtn = document.getElementById('btnToggleAvailability');
    const locationInputs = {
        lat: document.getElementById('driverAvailabilityLatitude'),
        lon: document.getElementById('driverAvailabilityLongitude')
    };
    const registrationNode = document.getElementById('driverAmbulanceRegistrationNumber');

    if (!statusBadge || !locationOut || !toggleBtn) return;

    const available = data?.availabilityStatus === 'AVAILABLE';
    statusBadge.textContent = available ? 'AVAILABLE' : 'UNAVAILABLE';
    statusBadge.className = `inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold ${available ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-200 text-slate-700'}`;
    locationOut.textContent = data?.latitude != null && data?.longitude != null
        ? `${data.latitude.toFixed(4)}, ${data.longitude.toFixed(4)}`
        : 'No live location';
    toggleBtn.textContent = available ? 'Set Unavailable' : 'Set Available';
    toggleBtn.className = `font-bold px-4 py-2 rounded text-sm transition ${available ? 'bg-slate-700 hover:bg-slate-800 text-white' : 'bg-emerald-600 hover:bg-emerald-700 text-white'}`;

    if (locationInputs.lat && locationInputs.lon) {
        if (data?.latitude != null) locationInputs.lat.value = data.latitude;
        if (data?.longitude != null) locationInputs.lon.value = data.longitude;
    }
    if (registrationNode) {
        registrationNode.textContent = data?.ambulanceRegistrationNumber || 'Not set';
    }
}

async function handleToggleAvailability() {
    if (!state.driverToken) return showAlert('Please log in as Driver first.', 'error');

    const currentStatus = driverAvailability?.availabilityStatus || 'UNAVAILABLE';
    const nextStatus = currentStatus === 'AVAILABLE' ? 'UNAVAILABLE' : 'AVAILABLE';
    const latValue = Number(document.getElementById('driverAvailabilityLatitude')?.value ?? 0);
    const lonValue = Number(document.getElementById('driverAvailabilityLongitude')?.value ?? 0);

    try {
        const result = await api.drivers.setAvailability(
            nextStatus,
            nextStatus === 'AVAILABLE' ? latValue : null,
            nextStatus === 'AVAILABLE' ? lonValue : null
        );
        driverAvailability = result;
        renderDriverAvailability(result);
        showAlert(`Driver status updated to ${result.availabilityStatus}.`, 'success');
    } catch (err) {
        showAlert('Failed to update availability: ' + err.message, 'error');
    }
}

async function handleRefreshLocation() {
    if (!state.driverToken) return showAlert('Please log in as Driver first.', 'error');

    const currentStatus = driverAvailability?.availabilityStatus || 'UNAVAILABLE';
    if (currentStatus !== 'AVAILABLE') {
        showAlert('You must mark yourself available before refreshing your location.', 'error');
        return;
    }

    const latValue = Number(document.getElementById('driverAvailabilityLatitude')?.value ?? 0);
    const lonValue = Number(document.getElementById('driverAvailabilityLongitude')?.value ?? 0);

    if (!latValue || !lonValue) {
        showAlert('Please enter both latitude and longitude before refreshing your location.', 'error');
        return;
    }

    try {
        const result = await api.drivers.updateLocation(latValue, lonValue);
        driverAvailability = result ?? driverAvailability;
        if (driverAvailability) {
            driverAvailability.latitude = latValue;
            driverAvailability.longitude = lonValue;
            driverAvailability.lastLocationUpdate = new Date().toISOString();
        }
        renderDriverAvailability(driverAvailability || {
            availabilityStatus: 'UNAVAILABLE',
            latitude: latValue,
            longitude: lonValue
        });
        showAlert('Location refreshed successfully.', 'success');
    } catch (err) {
        showAlert('Failed to refresh location: ' + err.message, 'error');
    }
}
