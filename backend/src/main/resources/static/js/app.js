/**
 * Master Application Orchestrator & Asynchronous HTML Component Loader
 */
import { state } from './state.js';
import { initEmergencyModule } from './modules/emergency.js';
import { initTrackingModule } from './modules/tracking.js';
import { initDriverModule, loadAvailableJobs, syncActiveTripFromBackend } from './modules/driver.js';
import { initDispatcherModule } from './modules/dispatcher.js';
import { initAdminModule, onAdminLogin } from './modules/admin.js';
import { initRegisterModule } from './modules/register.js';

// DOM Views
let viewEmergency, viewRegister, viewDriver, viewDispatcher, viewAdmin;

// Asynchronously load HTML partials into data-src containers
async function loadComponents() {
    const containers = document.querySelectorAll('[data-src]');
    const fetchPromises = Array.from(containers).map(async (container) => {
        try {
            const res = await fetch(container.dataset.src);
            if (!res.ok) throw new Error(`Failed to load ${container.dataset.src}`);
            container.innerHTML = await res.text();
        } catch (err) {
            console.error('Component load error:', err);
        }
    });
    await Promise.all(fetchPromises);
}

// Navigation Handlers
function setupNavigation() {
    viewEmergency = document.getElementById('viewEmergency');
    viewRegister = document.getElementById('viewRegister');
    viewDriver = document.getElementById('viewDriver');
    viewDispatcher = document.getElementById('viewDispatcher');
    viewAdmin = document.getElementById('viewAdmin');

    document.getElementById('navEmergency')?.addEventListener('click', () => switchView('emergency'));
    document.getElementById('navRegister')?.addEventListener('click', () => switchView('register'));
    document.getElementById('navDriver')?.addEventListener('click', () => switchView('driver'));
    document.getElementById('navDispatcher')?.addEventListener('click', () => switchView('dispatcher'));
    document.getElementById('navAdmin')?.addEventListener('click', () => switchView('admin'));
}

function switchView(view) {
    viewEmergency.classList.toggle('hidden', view !== 'emergency');
    viewRegister.classList.toggle('hidden', view !== 'register');
    viewDriver.classList.toggle('hidden', view !== 'driver');
    viewDispatcher.classList.toggle('hidden', view !== 'dispatcher');
    viewAdmin.classList.toggle('hidden', view !== 'admin');

    if (view === 'emergency') {
        setTimeout(() => {
            if (state.pickerMap) state.pickerMap.invalidateSize();
        }, 200);
    } else if (view === 'register') {
        // no-op: registration form does not require post-load refresh yet
    } else if (view === 'driver' && state.driverToken) {
        loadAvailableJobs();
        syncActiveTripFromBackend();
    } else if (view === 'admin' && state.adminToken) {
        onAdminLogin();
    }
}

// Global Lifecycle Bootstrapper
window.addEventListener('DOMContentLoaded', async () => {
    // 1. AWAIT the HTML fragments to ensure they are loaded before proceeding
    await loadComponents();

    // 2. Now that the DOM is fully populated, setup navigation and wire modules
    setupNavigation();
    initEmergencyModule();
    initTrackingModule();
    initDriverModule();
    initDispatcherModule();
    initAdminModule();
    initRegisterModule();

    // 3. Set the default starting view
    switchView('emergency');
});
