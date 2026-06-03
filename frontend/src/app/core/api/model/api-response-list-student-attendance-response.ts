/**
 * Training school system Backend API
 *
 * NOTE: This class is manually created to represent the backend model since Swagger is currently offline.
 */
import { ErrorDetail } from './error-detail';
import { StudentAttendanceResponse } from './student-attendance-response';

export interface ApiResponseListStudentAttendanceResponse { 
    success?: boolean;
    message?: string;
    errorCode?: string;
    data?: Array<StudentAttendanceResponse>;
    errors?: Array<ErrorDetail>;
    timestamp?: string;
}
