/**
 * Emergency Request View: Geolocation, OpenStreetMap Picker, and Dispatch Form
 */
import { state } from '../state.js';
import { api } from '../api.js';
import { showAlert } from '../ui.js';
import { startLiveTracking } from './tracking.js';

export function initEmergencyModule() {
    initPickerMap();

    document.getElementById('btnDetectGps')?.addEventListener('click', handleDetectGps);
    document.getElementById('emergencyForm')?.addEventListener('submit', handleEmergencySubmit);
}

function initPickerMap() {
    const defaultLat = 12.9716;
    const defaultLng = 77.5946;

    if (!document.getElementById('mapPicker')) return;

    state.pickerMap = L.map('mapPicker').setView([defaultLat, defaultLng], 13);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '© OpenStreetMap contributors'
    }).addTo(state.pickerMap);

    state.pickerMarker = L.marker([defaultLat, defaultLng], { draggable: true }).addTo(state.pickerMap);

    state.pickerMarker.on('dragend', (e) => {
        const coord = e.target.getLatLng();
        updatePickerCoordinates(coord.lat, coord.lng);
    });

    state.pickerMap.on('click', (e) => {
        state.pickerMarker.setLatLng(e.latlng);
        updatePickerCoordinates(e.latlng.lat, e.latlng.lng);
    });
}

function updatePickerCoordinates(lat, lng) {
    document.getElementById('pickupLat').value = lat.toFixed(6);
    document.getElementById('pickupLng').value = lng.toFixed(6);
    document.getElementById('lblLat').textContent = lat.toFixed(4);
    document.getElementById('lblLng').textContent = lng.toFixed(4);
}

function handleDetectGps() {
    if (!navigator.geolocation) {
        return showAlert('Geolocation is not supported by your browser.', 'error');
    }
    showAlert('Detecting GPS location...', 'info');

    navigator.geolocation.getCurrentPosition(
        (pos) => {
            const lat = pos.coords.latitude;
            const lng = pos.coords.longitude;
            state.pickerMap.setView([lat, lng], 15);
            state.pickerMarker.setLatLng([lat, lng]);
            updatePickerCoordinates(lat, lng);
            document.getElementById('pickupAddress').value = `Current Location (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
            showAlert('Location detected successfully!', 'success');
        },
        (err) => {
            showAlert('Could not retrieve GPS: ' + err.message, 'error');
        },
        { enableHighAccuracy: true, timeout: 10000 }
    );
}

async function handleEmergencySubmit(e) {
    e.preventDefault();

    const payload = {
        contactName: document.getElementById('contactName').value,
        contactPhone: document.getElementById('contactPhone').value,
        pickupAddress: document.getElementById('pickupAddress').value,
        pickupLatitude: parseFloat(document.getElementById('pickupLat').value),
        pickupLongitude: parseFloat(document.getElementById('pickupLng').value),
        destinationAddress: document.getElementById('destAddress').value,
        destinationLatitude: parseFloat(document.getElementById('destLat').value),
        destinationLongitude: parseFloat(document.getElementById('destLng').value),
        patientCondition: document.getElementById('patientCondition').value,
        ambulanceType: document.getElementById('ambulanceType').value,
        bookingChannel: 'ONLINE_WEB'
    };

    try {
        const data = await api.bookings.createEmergency(payload);
        const sourceLabel = data.recordedByName
            ? `recorded by ${data.recordedByName}`
            : 'submitted directly by you';
        showAlert(`Ambulance Dispatched! Your Booking ID is #${data.id} (${sourceLabel}).`, 'success');

        document.getElementById('trackBookingId').value = data.id;
        startLiveTracking(data.id, payload.pickupLatitude, payload.pickupLongitude);
    } catch (err) {
        showAlert('Dispatch failed: ' + err.message, 'error');
    }
}
