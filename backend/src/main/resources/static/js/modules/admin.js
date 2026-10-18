import { state, setAdminSession } from '../state.js';
import { api } from '../api.js';
import { showAlert } from '../ui.js';

// DOM Elements for this module
let pendingUsersList;
let superAdminSection;
let allUsersList;
let adminPanelContent;
let adminPanelLoginRequired;
let adminPanelAuthBadge;
let adminPanelLogoutBtn;
let adminPanelLoginForm;
let adminPanelEmail;
let adminPanelPassword;

function isSuperAdmin() {
    return state.adminRole === 'ROLE_SUPER_ADMIN';
}

function formatStatusLabel(status) {
    return (status || 'UNKNOWN').replace(/_/g, ' ');
}

function getStatusClass(status) {
    return {
        ACTIVE: 'bg-green-100 text-green-800',
        PENDING: 'bg-yellow-100 text-yellow-800',
        SUSPENDED: 'bg-orange-100 text-orange-800',
        REJECTED: 'bg-red-100 text-red-800'
    }[status] || 'bg-slate-100 text-slate-700';
}

function getManageActionButtons(user) {
    if (user.status === 'PENDING') {
        return `
            <button data-action="approve" data-user-id="${user.id}" class="admin-user-action-btn bg-green-600 hover:bg-green-700 text-white font-semibold px-3 py-1 text-xs rounded transition">
                Approve
            </button>
            <button data-action="reject" data-user-id="${user.id}" class="admin-user-action-btn bg-red-600 hover:bg-red-700 text-white font-semibold px-3 py-1 text-xs rounded transition">
                Reject
            </button>
        `;
    }

    if (user.status === 'ACTIVE') {
        return `
            <button data-action="suspend" data-user-id="${user.id}" class="admin-user-action-btn bg-orange-600 hover:bg-orange-700 text-white font-semibold px-3 py-1 text-xs rounded transition">
                Suspend
            </button>
        `;
    }

    if (user.status === 'SUSPENDED') {
        return `
            <button data-action="reactivate" data-user-id="${user.id}" class="admin-user-action-btn bg-blue-600 hover:bg-blue-700 text-white font-semibold px-3 py-1 text-xs rounded transition">
                Reactivate
            </button>
        `;
    }

    return '<span class="text-xs text-slate-400">No actions</span>';
}

function renderPendingUsers(users) {
    if (!pendingUsersList) return;

    pendingUsersList.innerHTML = '';
    if (users.length === 0) {
        pendingUsersList.innerHTML = '<p class="text-sm text-slate-500 p-2">No users are currently pending approval.</p>';
        return;
    }

    users.forEach(user => {
        const userCard = document.createElement('div');
        userCard.className = 'flex justify-between items-center bg-white p-2 rounded border';
        userCard.innerHTML = `
            <div>
                <p class="font-semibold">${user.fullName} <span class="text-xs text-slate-500">(${user.email})</span></p>
                <p class="text-xs font-mono px-1.5 py-0.5 bg-yellow-100 text-yellow-800 rounded-full inline-block">${user.role}</p>
            </div>
            <div class="flex gap-2">
                <button data-action="approve" data-user-id="${user.id}" class="pending-user-action-btn bg-green-600 hover:bg-green-700 text-white font-bold px-3 py-1 text-xs rounded transition">
                    Approve
                </button>
                <button data-action="reject" data-user-id="${user.id}" class="pending-user-action-btn bg-red-600 hover:bg-red-700 text-white font-bold px-3 py-1 text-xs rounded transition">
                    Reject
                </button>
            </div>
        `;
        pendingUsersList.appendChild(userCard);
    });
}

function renderAllUsers(users) {
    if (!allUsersList) return;

    allUsersList.innerHTML = '';
    if (users.length === 0) {
        allUsersList.innerHTML = '<p class="text-sm text-slate-500 p-2">No users found in the system.</p>';
        return;
    }

    users.forEach(user => {
        const statusLabel = formatStatusLabel(user.status);
        const statusClass = getStatusClass(user.status);

        const userCard = document.createElement('div');
        userCard.className = 'flex justify-between items-center gap-3 bg-white p-3 rounded border';
        userCard.innerHTML = `
            <div>
                <p class="font-semibold">${user.fullName} <span class="text-xs text-slate-500">(${user.email})</span></p>
                <div class="mt-1 flex flex-wrap gap-2 items-center">
                    <p class="text-xs font-mono px-1.5 py-0.5 bg-slate-100 text-slate-700 rounded-full inline-block">${user.role}</p>
                    <span class="text-xs text-slate-500">${user.phone}</span>
                </div>
            </div>
            <div class="text-right text-xs text-slate-500 space-y-2">
                <div class="inline-block px-2 py-0.5 rounded-full ${statusClass}">${statusLabel}</div>
                <div class="flex justify-end gap-2">
                    ${getManageActionButtons(user)}
                </div>
            </div>
        `;
        allUsersList.appendChild(userCard);
    });
}

async function fetchAndRenderPendingUsers() {
    if (!state.adminToken || !pendingUsersList) return;

    try {
        const users = await api.admin.getPendingUsers();
        renderPendingUsers(users);
    } catch (error) {
        showAlert(`Error fetching pending users: ${error.message}`, 'error');
    }
}

async function fetchAndRenderAllUsers() {
    if (!state.adminToken || !allUsersList) return;

    try {
        const users = await api.admin.getAllUsers();
        renderAllUsers(users);
    } catch (error) {
        showAlert(`Error fetching all users: ${error.message}`, 'error');
    }
}

async function approveUser(userId) {
    try {
        await api.admin.approveUser(userId);
        showAlert(`User #${userId} has been approved successfully!`, 'success');
        await refreshAdminData();
    } catch (error) {
        showAlert(`Error approving user: ${error.message}`, 'error');
    }
}

async function rejectUser(userId) {
    try {
        await api.admin.rejectUser(userId);
        showAlert(`User #${userId} has been rejected.`, 'info');
        await refreshAdminData();
    } catch (error) {
        showAlert(`Error rejecting user: ${error.message}`, 'error');
    }
}

async function suspendUser(userId) {
    try {
        await api.admin.suspendUser(userId);
        showAlert(`User #${userId} has been suspended.`, 'info');
        await refreshAdminData();
    } catch (error) {
        showAlert(`Error suspending user: ${error.message}`, 'error');
    }
}

async function reactivateUser(userId) {
    try {
        await api.admin.reactivateUser(userId);
        showAlert(`User #${userId} has been reactivated.`, 'success');
        await refreshAdminData();
    } catch (error) {
        showAlert(`Error reactivating user: ${error.message}`, 'error');
    }
}

async function refreshAdminData() {
    await fetchAndRenderPendingUsers();
    await fetchAndRenderAllUsers();
}

function handleUserActionClick(target) {
    const userId = target?.dataset?.userId;
    const action = target?.dataset?.action;

    if (!userId || !action) return;

    if (action === 'approve') {
        approveUser(userId);
    } else if (action === 'reject') {
        rejectUser(userId);
    } else if (action === 'suspend') {
        suspendUser(userId);
    } else if (action === 'reactivate') {
        reactivateUser(userId);
    }
}

function updateAdminAuthUI() {
    if (!adminPanelLoginRequired || !adminPanelContent || !adminPanelAuthBadge) return;

    const roleLabel = state.adminRole ? state.adminRole.replace(/^ROLE_/, '').replace(/_/g, ' ') : 'Admin';

    if (state.adminToken) {
        adminPanelAuthBadge.textContent = `Logged in as: ${state.adminName || 'Admin'} (${roleLabel})`;
        adminPanelLogoutBtn?.classList.remove('hidden');
        adminPanelLoginRequired.classList.add('hidden');
        adminPanelContent.classList.remove('hidden');

        if (superAdminSection) {
            superAdminSection.classList.remove('hidden');
        }
    } else {
        adminPanelAuthBadge.textContent = 'Not logged in';
        adminPanelLogoutBtn?.classList.add('hidden');
        adminPanelLoginRequired.classList.remove('hidden');
        adminPanelContent.classList.add('hidden');

        if (superAdminSection) {
            superAdminSection.classList.add('hidden');
        }
    }
}

async function handleAdminLogin(e) {
    e.preventDefault();

    const email = adminPanelEmail?.value.trim();
    const password = adminPanelPassword?.value;

    if (!email || !password) {
        showAlert('Please enter your admin email and password.', 'error');
        return;
    }

    try {
        const data = await api.auth.login(email, password);
        if (!['ROLE_ADMIN', 'ROLE_SUPER_ADMIN'].includes(data.role)) {
            throw new Error('You are not authorized to access the admin panel.');
        }
        setAdminSession(data.token, data.fullName, data.role);
        adminPanelLoginForm?.reset();
        updateAdminAuthUI();
        showAlert(`Welcome, ${data.fullName}!`, 'success');
        await refreshAdminData();
    } catch (error) {
        showAlert(`Admin login failed: ${error.message}`, 'error');
    }
}

function handleAdminLogout() {
    setAdminSession(null, null, null);
    adminPanelLoginForm?.reset();
    updateAdminAuthUI();
    pendingUsersList.innerHTML = '';
    if (allUsersList) {
        allUsersList.innerHTML = '';
    }
    showAlert('Admin logged out.', 'info');
}

export function initAdminModule() {
    // Find elements once the component is loaded
    pendingUsersList = document.getElementById('pending-users-list');
    superAdminSection = document.getElementById('super-admin-section');
    allUsersList = document.getElementById('all-users-list');
    adminPanelContent = document.getElementById('adminPanelContent');
    adminPanelLoginRequired = document.getElementById('adminPanelLoginRequired');
    adminPanelAuthBadge = document.getElementById('adminPanelAuthBadge');
    adminPanelLogoutBtn = document.getElementById('btnAdminPanelLogout');
    adminPanelLoginForm = document.getElementById('adminPanelLoginForm');
    adminPanelEmail = document.getElementById('adminPanelEmail');
    adminPanelPassword = document.getElementById('adminPanelPassword');

    // Attach event listeners
    document.getElementById('btnRefreshPendingUsers')?.addEventListener('click', fetchAndRenderPendingUsers);
    document.getElementById('btnRefreshAllUsers')?.addEventListener('click', fetchAndRenderAllUsers);
    adminPanelLoginForm?.addEventListener('submit', handleAdminLogin);
    adminPanelLogoutBtn?.addEventListener('click', handleAdminLogout);

    pendingUsersList?.addEventListener('click', (e) => {
        if (e.target && e.target.classList.contains('pending-user-action-btn')) {
            handleUserActionClick(e.target);
        }
    });

    allUsersList?.addEventListener('click', (e) => {
        if (e.target && e.target.classList.contains('admin-user-action-btn')) {
            handleUserActionClick(e.target);
        }
    });

    updateAdminAuthUI();

    if (state.adminToken) {
        refreshAdminData();
    }
}

// This function will be called when the admin/dispatcher logs in
export function onAdminLogin() {
    if (!state.adminToken) return;

    updateAdminAuthUI();

    // Fetch initial data
    refreshAdminData();

    // Check if the logged-in user is a SUPER_ADMIN to show extra sections
    // This assumes the role is available in the login response or JWT payload
    // For now, we'll just keep it simple and not show the super-admin section.
}

export function onAdminLogout() {
    updateAdminAuthUI();
    if (pendingUsersList) pendingUsersList.innerHTML = '';
    if (allUsersList) allUsersList.innerHTML = '';
}
