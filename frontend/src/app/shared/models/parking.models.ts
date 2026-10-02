export interface SlotDto {
  id: number;
  slotNumber: string;
  occupied: boolean;
}

export interface SectionDto {
  id: number;
  sectionName: string;
  floor: string;
  vehicleType: 'CAR' | 'BIKE' | 'TRUCK';
  available: number;
  total: number;
  slots: SlotDto[];
}

export interface VehicleTypeDto {
  type: 'CAR' | 'BIKE' | 'TRUCK';
  available: number;
  total: number;
}

export interface SummaryDto {
  availableSlots: number;
  totalCapacity: number;
  occupancyRate: number;
  parkingFloors: number;
  lotName: string;
  address: string;
  rating: number;
}

export interface RateDto {
  vehicleType: string;
  ratePerHour: number;
}

export interface DashboardResponse {
  summary: SummaryDto;
  vehicleTypeSummary: VehicleTypeDto[];
  sections: SectionDto[];
  rates: RateDto[];
}

// GET /api/parking/slots/{id} — used by the booking page deep link.
export interface SlotDetail {
  id: number;
  slotNumber: string;
  displayNumber: string;
  sectionName: string;
  floorName: string;
  vehicleType: 'CAR' | 'BIKE' | 'TRUCK';
  occupied: boolean;
  ratePerHour: number;
  lotName: string;
  lotAddress: string;
}