/**
 * Dispatcher Console: Login and Phone Intake Logging
 */
import { state, setAdminSession } from '../state.js';
import { api } from '../api.js';
import { showAlert } from '../ui.js';

export function initDispatcherModule() {
    updateAdminAuthUI();

    document.getElementById('btnAdminLogin')?.addEventListener('click', handleAdminLogin);
    document.getElementById('btnAdminLogout')?.addEventListener('click', handleAdminLogout);
    document.getElementById('phoneDispatchForm')?.addEventListener('submit', handlePhoneDispatchSubmit);
}

export function updateAdminAuthUI() {
    const authBadge = document.getElementById('adminAuthBadge');
    const logoutBtn = document.getElementById('btnAdminLogout');
    const loginBox = document.getElementById('adminLoginBox');

    if (state.adminToken) {
        authBadge.textContent = `Logged in as: ${state.adminName || 'Dispatcher'}`;
        logoutBtn.classList.remove('hidden');
        loginBox.classList.add('hidden');
    } else {
        authBadge.textContent = 'Not Logged In';
        logoutBtn.classList.add('hidden');
        loginBox.classList.remove('hidden');
    }
}

async function handleAdminLogin() {
    const email = document.getElementById('adminEmail').value;
    const password = document.getElementById('adminPassword').value;

    try {
        const data = await api.auth.login(email, password);
        setAdminSession(data.token, data.fullName);
        updateAdminAuthUI();
        showAlert('Dispatcher logged in!', 'success');
    } catch (err) {
        showAlert('Dispatcher login failed: ' + err.message, 'error');
    }
}

function handleAdminLogout() {
    setAdminSession(null, null);
    updateAdminAuthUI();
    showAlert('Dispatcher logged out.', 'info');
}

async function handlePhoneDispatchSubmit(e) {
    e.preventDefault();
    if (!state.adminToken) return showAlert('Please log in as Dispatcher first.', 'error');

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
        showAlert(`Call Intake Recorded! Booking #${data.id} created.`, 'success');
        document.getElementById('phoneDispatchForm').reset();
    } catch (err) {
        showAlert('Dispatch call failed: ' + err.message, 'error');
    }
}
