package com.parking.inventory.model;

/**
 * Shared vehicle-type enum used across the inventory, booking, and operator
 * modules. Previously each module (dashboard.ParkingSection, booking.Booking,
 * operator.ParkingTicket) declared its own copy of the same three constants —
 * consolidated here so a slot, a booking, and a ticket always agree on the
 * same set of types.
 */
public enum VehicleType {
    CAR, BIKE, TRUCK
}
