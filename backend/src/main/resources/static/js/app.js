/**
 * Master Application Orchestrator & Asynchronous HTML Component Loader
 */
import { state } from './state.js';
import { initEmergencyModule } from './modules/emergency.js';
import { initTrackingModule } from './modules/tracking.js';
import { initDriverModule, loadAvailableJobs, syncActiveTripFromBackend } from './modules/driver.js';
import { initDispatcherModule } from './modules/dispatcher.js';

// DOM Views
const viewEmergency = document.getElementById('viewEmergency');
const viewDriver = document.getElementById('viewDriver');
const viewDispatcher = document.getElementById('viewDispatcher');

// Asynchronously load HTML partials into data-src containers
async function loadComponents() {
    const containers = document.querySelectorAll('[data-src]');
    for (const container of containers) {
        try {
            const res = await fetch(container.dataset.src);
            if (!res.ok) throw new Error(`Failed to load ${container.dataset.src}`);
            container.innerHTML = await res.text();
        } catch (err) {
            console.error('Component load error:', err);
        }
    }
}

// Navigation Handlers
document.getElementById('navEmergency')?.addEventListener('click', () => switchView('emergency'));
document.getElementById('navDriver')?.addEventListener('click', () => switchView('driver'));
document.getElementById('navDispatcher')?.addEventListener('click', () => switchView('dispatcher'));

function switchView(view) {
    viewEmergency.classList.toggle('hidden', view !== 'emergency');
    viewDriver.classList.toggle('hidden', view !== 'driver');
    viewDispatcher.classList.toggle('hidden', view !== 'dispatcher');

    if (view === 'emergency') {
        setTimeout(() => {
            if (state.pickerMap) state.pickerMap.invalidateSize();
        }, 200);
    } else if (view === 'driver' && state.driverToken) {
        loadAvailableJobs();
        syncActiveTripFromBackend();
    }
}

// Global Lifecycle Bootstrapper
window.addEventListener('DOMContentLoaded', async () => {
    // 1. Load HTML fragments first
    await loadComponents();

    // 2. Wire event listeners and modules once DOM elements are present
    initEmergencyModule();
    initTrackingModule();
    initDriverModule();
    initDispatcherModule();
});
