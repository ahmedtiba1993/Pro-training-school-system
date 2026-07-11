import { ErrorDetail } from './error-detail';
import { FinancialContractListResponse } from './financial-contract-list-response';

export interface ApiResponseListFinancialContractListResponse { 
    success?: boolean;
    message?: string;
    errorCode?: string;
    data?: Array<FinancialContractListResponse>;
    errors?: Array<ErrorDetail>;
    timestamp?: string;
}
