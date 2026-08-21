export enum VehicleType {
  CAR = 'CAR',
  BIKE = 'BIKE',
  TRUCK = 'TRUCK'
}

export enum BookingStatus {
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export interface BookingResponse {
  id: number;
  bookingRef: string;
  userId: number;
  slotId: number;
  slotNumber: string;
  sectionName: string;
  floorName: string;
  lotName: string;
  lotAddress: string;
  vehicleNumber: string;
  vehicleLabel: string;
  vehicleType: VehicleType;
  entryDate: string;
  entryTime: string;
  durationHours: number;
  ratePerHour: number;
  serviceFee: number;
  totalAmount: number;
  status: BookingStatus;
  qrData: string;
  cancelledAt: string | null;
  createdAt: string;
}

// Only the slot, the vehicle plate, and the duration are sent — the backend
// resolves section/floor/lot/rate from the real slot and validates the
// vehicle against the caller's own registered vehicles. Previously this
// request carried all of that as freeform client-supplied fields with no
// link back to real inventory.
export interface CreateBookingRequest {
  slotId: number;
  vehicleNumber: string;
  durationHours: number;
}

export interface MyBookingsSummary {
  activeCount: number;
  totalCount: number;
  totalSpent: number;
  active: BookingResponse[];
  history: BookingResponse[];
}

export interface VehicleDto {
  id: number;
  vehicleNumber: string;
  vehicleLabel: string;
  vehicleType: VehicleType;
}

// Passed via router state from a dashboard slot click.
export interface SlotContext {
  slotId: number;
  slotNumber: string;
  sectionName: string;
  floorName: string;
  lotName: string;
  lotAddress: string;
  vehicleType: VehicleType;
  ratePerHour: number;
}
