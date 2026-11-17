import React, { useState } from 'react';
import { useKeycloak } from '@react-keycloak/web';

const ReportPage: React.FC = () => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [dateFrom, setDateFrom] = useState('');
    const [dateTo, setDateTo] = useState('');

    const {keycloak, initialized} = useKeycloak();

    const downloadReport = async () => {
        if (!keycloak?.token) {
            setError('Not authenticated');
            return;
        }

        if (dateFrom && dateTo && dateFrom > dateTo) {
            setError('Start date cannot be later than end date');
            return;
        }

        try {
            setLoading(true);
            setError(null);

            const params = new URLSearchParams();

            if (dateFrom) {
                params.append('dateFrom', dateFrom);
            }

            if (dateTo) {
                params.append('dateTo', dateTo);
            }

            const queryString = params.toString();
            const url = `${process.env.REACT_APP_API_URL}/reports${queryString ? `?${queryString}` : ''}`;

            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${keycloak.token}`
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to download report: ${response.status}`);
            }

            const blob = await response.blob();

            const downloadUrl = window.URL.createObjectURL(blob);
            const link = document.createElement('a');

            link.href = downloadUrl;
            link.download = `report_${dateFrom || 'all'}_${dateTo || 'all'}.json`;

            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

            window.URL.revokeObjectURL(downloadUrl);

        } catch (err) {
            setError(err instanceof Error ? err.message : 'An error occurred');
        } finally {
            setLoading(false);
        }
    };

    if (!initialized) {
        return <div>Loading...</div>;
    }

    if (!keycloak.authenticated) {
        return (
            <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100">
                <button
                    onClick={() => keycloak.login()}
                    className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
                >
                    Login
                </button>
            </div>
        );
    }

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100">
            <div className="p-8 bg-white rounded-lg shadow-md w-full max-w-md">
                <h1 className="text-2xl font-bold mb-6">Usage Reports</h1>

                <div className="flex flex-col gap-4 mb-6">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Date from
                        </label>
                        <input
                            type="date"
                            value={dateFrom}
                            max={dateTo || undefined}
                            onChange={(e) => setDateFrom(e.target.value)}
                            className="w-full px-3 py-2 border rounded"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                            Date to
                        </label>
                        <input
                            type="date"
                            value={dateTo}
                            min={dateFrom || undefined}
                            onChange={(e) => setDateTo(e.target.value)}
                            className="w-full px-3 py-2 border rounded"
                        />
                    </div>
                </div>

                <button
                    onClick={downloadReport}
                    disabled={loading}
                    className={`w-full px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 ${
                        loading ? 'opacity-50 cursor-not-allowed' : ''
                    }`}
                >
                    {loading ? 'Generating Report...' : 'Download Report'}
                </button>

                {error && (
                    <div className="mt-4 p-4 bg-red-100 text-red-700 rounded">
                        {error}
                    </div>
                )}
            </div>
        </div>
    );
}

export default ReportPage;