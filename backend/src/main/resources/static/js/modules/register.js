import { api } from '../api.js';
import { showAlert } from '../ui.js';

let registerForm;
let registerRole;
let registerRoleHint;
let registerAddress;
let registerAddressHint;
let registerInviteGroup;
let registerInviteCode;
let registerTerms;

function getSelectedRole() {
    return registerRole?.value || 'ROLE_USER';
}

function updateRoleUI() {
    if (!registerRole || !registerAddress || !registerInviteGroup || !registerRoleHint || !registerAddressHint) return;

    const role = getSelectedRole();
    const isStaffRole = role === 'ROLE_DRIVER' || role === 'ROLE_DISPATCHER';
    const isAdminRole = role === 'ROLE_ADMIN';

    registerAddress.required = isStaffRole;
    registerInviteGroup.classList.toggle('hidden', !isAdminRole);
    if (registerInviteCode) {
        registerInviteCode.required = isAdminRole;
    }

    if (role === 'ROLE_DRIVER') {
        registerRoleHint.textContent = 'Driver registrations require an address and approval.';
        registerAddressHint.textContent = 'Address required for driver onboarding and service coverage.';
    } else if (role === 'ROLE_DISPATCHER') {
        registerRoleHint.textContent = 'Dispatcher registrations require an address and approval.';
        registerAddressHint.textContent = 'Address required for dispatcher onboarding and service coverage.';
    } else if (role === 'ROLE_ADMIN') {
        registerRoleHint.textContent = 'Admin registrations are invite-only.';
        registerAddressHint.textContent = 'Address is optional for invite-based admin registration.';
    } else {
        registerRoleHint.textContent = 'For patients, family members, and public users.';
        registerAddressHint.textContent = 'Optional for users. Required for drivers and dispatchers.';
    }
}

function validateForm() {
    const fullName = document.getElementById('registerFullName').value.trim();
    const email = document.getElementById('registerEmail').value.trim();
    const phone = document.getElementById('registerPhone').value.trim();
    const password = document.getElementById('registerPassword').value;
    const confirmPassword = document.getElementById('registerConfirmPassword').value;
    const address = registerAddress.value.trim();
    const inviteCode = registerInviteCode?.value.trim() || '';
    const role = getSelectedRole();

    if (!fullName || !email || !phone || !password || !confirmPassword) {
        showAlert('Please complete all required registration fields.', 'error');
        return null;
    }

    if (password !== confirmPassword) {
        showAlert('Passwords do not match.', 'error');
        return null;
    }

    if ((role === 'ROLE_DRIVER' || role === 'ROLE_DISPATCHER') && !address) {
        showAlert('Address is required for drivers and dispatchers.', 'error');
        return null;
    }

    if (role === 'ROLE_ADMIN' && !inviteCode) {
        showAlert('Admin registration requires an invitation code.', 'error');
        return null;
    }

    return {
        fullName,
        email,
        phone,
        password,
        confirmPassword,
        role,
        address: address || null,
        inviteCode: inviteCode || null
    };
}

async function handleRegisterSubmit(e) {
    e.preventDefault();

    const payload = validateForm();
    if (!payload) return;

    try {
        const response = await api.auth.register(payload);
        registerForm.reset();
        updateRoleUI();

        if (response.token) {
            showAlert(`Account created successfully for ${response.fullName}. You can now log in.`, 'success');
        } else {
            const approvalLabel = response.role === 'ROLE_DRIVER' || response.role === 'ROLE_DISPATCHER'
                ? 'Your account is pending approval.'
                : 'Your account has been created.';
            showAlert(`${approvalLabel} Please wait for the approval process to complete.`, 'success');
        }
    } catch (error) {
        showAlert(`Registration failed: ${error.message}`, 'error');
    }
}

export function initRegisterModule() {
    registerForm = document.getElementById('registerForm');
    registerRole = document.getElementById('registerRole');
    registerRoleHint = document.getElementById('registerRoleHint');
    registerAddress = document.getElementById('registerAddress');
    registerAddressHint = document.getElementById('registerAddressHint');
    registerInviteGroup = document.getElementById('registerInviteGroup');
    registerInviteCode = document.getElementById('registerInviteCode');
    registerTerms = document.getElementById('registerTerms');

    if (!registerForm) return;

    registerRole?.addEventListener('change', updateRoleUI);
    registerForm.addEventListener('submit', handleRegisterSubmit);
    updateRoleUI();
}


