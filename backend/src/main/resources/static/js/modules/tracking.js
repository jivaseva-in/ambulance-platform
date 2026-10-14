/**
 * Live Telemetry & OpenStreetMap Route Tracking
 */
import { state } from '../state.js';
import { api } from '../api.js';
import { showAlert } from '../ui.js';

export function initTrackingModule() {
    document.getElementById('btnTrack')?.addEventListener('click', () => {
        const id = document.getElementById('trackBookingId').value;
        if (!id) return showAlert('Please enter a Booking ID', 'error');
        startLiveTracking(id);
    });
}

function initTrackingMap(lat, lng) {
    if (!state.trackMap) {
        state.trackMap = L.map('trackingMap').setView([lat, lng], 14);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '© OpenStreetMap'
        }).addTo(state.trackMap);
    } else {
        state.trackMap.setView([lat, lng], 14);
    }

    if (!state.trackPatientMarker) {
        state.trackPatientMarker = L.circleMarker([lat, lng], {
            radius: 8,
            fillColor: '#e11d48',
            color: '#ffffff',
            weight: 2,
            fillOpacity: 0.9
        }).addTo(state.trackMap).bindPopup('Patient Pickup Point');
    } else {
        state.trackPatientMarker.setLatLng([lat, lng]);
    }
}

export function startLiveTracking(bookingId, pickupLat = 12.9716, pickupLng = 77.5946) {
    state.activeBookingId = bookingId;
    document.getElementById('trackingInfo')?.classList.remove('hidden');

    setTimeout(() => {
        initTrackingMap(pickupLat, pickupLng);
        if (state.trackMap) state.trackMap.invalidateSize();
    }, 200);

    if (state.trackingInterval) clearInterval(state.trackingInterval);

    async function poll() {
        try {
            const booking = await api.bookings.getById(bookingId);
            document.getElementById('trackStatusBadge').textContent = booking.status;
            document.getElementById('trackDriverName').textContent = booking.assignedDriverName || 'Pending driver allocation...';

            try {
                const loc = await api.bookings.getLocation(bookingId);
                document.getElementById('trackSpeed').textContent = (loc.currentSpeedKmh || 0).toFixed(1) + ' km/h';

                if (loc.currentLatitude && loc.currentLongitude) {
                    const ambulanceCoord = [loc.currentLatitude, loc.currentLongitude];

                    if (!state.trackAmbulanceMarker) {
                        const ambIcon = L.divIcon({
                            className: 'custom-amb-icon',
                            html: '<div style="font-size: 24px;">🚑</div>',
                            iconSize: [30, 30],
                            iconAnchor: [15, 15]
                        });
                        state.trackAmbulanceMarker = L.marker(ambulanceCoord, { icon: ambIcon }).addTo(state.trackMap);
                        state.trackAmbulanceMarker.bindPopup('Ambulance Location');
                    } else {
                        state.trackAmbulanceMarker.setLatLng(ambulanceCoord);
                    }
                    state.trackMap.panTo(ambulanceCoord);
                }
            } catch (ignored) {}

            if (booking.status === 'COMPLETED' || booking.status === 'CANCELLED') {
                clearInterval(state.trackingInterval);
            }
        } catch (e) {
            console.error('Tracking polling error:', e);
        }
    }

    poll();
    state.trackingInterval = setInterval(poll, 3000);
}
