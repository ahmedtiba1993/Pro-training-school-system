/**
 * Training school system Backend API
 *
 * NOTE: This class is manually created to represent the backend model since Swagger is currently offline.
 */

export interface StudentAttendanceResponse { 
    enrollmentId?: number;
    firstName?: string;
    lastName?: string;
    studentCode?: string;
    status?: StudentAttendanceResponse.StatusEnum;
}
export namespace StudentAttendanceResponse {
    export const StatusEnum = {
        Absent: 'ABSENT',
        Present: 'PRESENT',
        Excused: 'EXCUSED'
    } as const;
    export type StatusEnum = typeof StatusEnum[keyof typeof StatusEnum];
}
