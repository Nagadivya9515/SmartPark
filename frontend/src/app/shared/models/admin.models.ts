export interface AdminLoginRequest {
    username: string;
    password: string;
    rememberMe: boolean;
  }
  
  export interface AdminLoginResponse {
    token: string;
    username: string;
    fullName: string;
    role: string;
    message: string;
  }
  
  export interface DailyPoint {
    date: string;
    value: number;
  }
  
  export interface OverviewResponse {
    todayRevenue: number;
    yesterdayRevenue: number;
    activeVehicles: number;
    totalSlots: number;
    availableSlots: number;
    activeOperators: number;
    totalOperators: number;
    occupancyRate: number;
    revenueTrend7d: DailyPoint[];
    trafficTrend7d: DailyPoint[];
    occupancyByType: Record<string, number>;
  }
  
  export interface ParkingLotDto {
    id: number;
    name: string;
    address: string;
    totalSlots: number;
    availableSlots: number;
    rating: number;
    active: boolean;
    carRate: number;
    bikeRate: number;
    truckRate: number;
  }
  
  export interface OperatorDto {
    id: number;
    operatorId: string;
    fullName: string;
    email: string;
    phone: string;
    role: string;
    gateName: string;
    assignedLot: string;
    active: boolean;
    initials: string;
  }
  
  export interface OperatorSummaryResponse {
    total: number;
    active: number;
    inactive: number;
    entryAndExit: number;
    operators: OperatorDto[];
  }
  
  export interface ReportSummaryResponse {
    totalRevenue: number;
    totalVehicles: number;
    avgRevenuePerDay: number;
    avgVehiclesPerDay: number;
    revenueTrend: number;
    trafficTrend: number;
    revenueTrend7d: DailyPoint[];
    trafficTrend7d: DailyPoint[];
    revenueByPayment: Record<string, number>;
    revenueByVehicle: Record<string, number>;
  }