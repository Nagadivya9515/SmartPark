export type VehicleType = 'CAR' | 'BIKE' | 'TRUCK';

export interface EntryRequest {
  vehicleNumber: string;
  vehicleType: VehicleType;
  gateName: string;
}
 

export interface LoginRequest {
  operatorId: string;
  password: string;
  rememberMe: boolean;
}

export interface LoginResponse {
  token: string;
  operatorId: string;
  fullName: string;
  gateName: string;
  lotName: string;
  role: string;
  message: string;
}

export interface TicketResponse {
  id: number;
  ticketId: string;
  vehicleNumber: string;
  vehicleType: string;
  slotNumber: string;
  sectionName: string;
  floorName: string;
  gateName: string;
  entryDate: string;
  entryTime: string;
  exitDate?: string;
  exitTime?: string;
  durationHours?: number;
  ratePerHour: number;
  parkingFee?: number;
  serviceFee?: number;
  totalAmount?: number;
  paymentMethod?: string;
  status: string;
  qrData: string;
}

export interface SlotDto {
  id: number;
  slotNumber: string;
  displayNumber: string;
  sectionName: string;
  floorName: string;
  vehicleType: string;
  occupied: boolean;
  currentTicketId: string | null;
}

export interface SectionDto {
  sectionName: string;
  vehicleType: string;
  available: number;
  occupied: number;
  slots: SlotDto[];
}

export interface FloorDto {
  floorName: string;
  totalSlots: number;
  availableSlots: number;
  occupiedSlots: number;
  occupancyRate: number;
  sections: SectionDto[];
}

export interface LiveStatusResponse {
  totalSlots: number;
  occupied: number;
  available: number;
  occupancyRate: number;
  todayEntries: number;
  availableByType: Record<string, number>;
  floors: FloorDto[];
}

export interface ExitStatsResponse {
  totalExits: number;
  revenueCollected: number;
  avgDurationHours: number;
}