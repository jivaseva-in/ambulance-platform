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

function formatLocationText(lat, lng) {
    if (lat == null || lng == null) return 'Waiting for GPS';
    return `${Number(lat).toFixed(4)}, ${Number(lng).toFixed(4)}`;
}

function updateTrackingContactInfo(booking, loc) {
    const driverNameEl = document.getElementById('trackDriverName');
    const driverPhoneLinkEl = document.getElementById('trackDriverPhoneLink');
    const currentLocationEl = document.getElementById('trackCurrentLocation');
    const speedEl = document.getElementById('trackSpeed');

    if (driverNameEl) {
        driverNameEl.textContent = booking.assignedDriverName || 'Pending allocation...';
    }

    const driverPhone = booking.driverPhone || 'Not assigned yet';
    if (driverPhoneLinkEl) {
        if (driverPhone && driverPhone !== 'Not assigned yet') {
            driverPhoneLinkEl.href = `tel:${driverPhone}`;
            driverPhoneLinkEl.textContent = driverPhone;
            driverPhoneLinkEl.classList.remove('pointer-events-none', 'text-slate-500');
            driverPhoneLinkEl.classList.add('text-blue-700');
        } else {
            driverPhoneLinkEl.href = '#';
            driverPhoneLinkEl.textContent = 'Not assigned yet';
            driverPhoneLinkEl.classList.add('pointer-events-none', 'text-slate-500');
            driverPhoneLinkEl.classList.remove('text-blue-700');
        }
    }

    if (currentLocationEl) {
        const lat = loc?.currentLatitude ?? booking.pickupLatitude ?? null;
        const lng = loc?.currentLongitude ?? booking.pickupLongitude ?? null;
        currentLocationEl.textContent = formatLocationText(lat, lng);
    }

    if (speedEl) {
        speedEl.textContent = `${(loc?.currentSpeedKmh || 0).toFixed(1)} km/h`;
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
            updateTrackingContactInfo(booking, null);

            try {
                const loc = await api.bookings.getLocation(bookingId);
                updateTrackingContactInfo(booking, loc);

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
