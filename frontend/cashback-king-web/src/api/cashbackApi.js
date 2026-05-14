const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

export const syncUserWithBackend = async (idToken) => {
    try {
        const response = await fetch(`${BASE_URL}/api/v1/users/sync`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${idToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
             // Attempt to get error details from the server response body
                        const errorData = await response.json().catch(() => ({}));
                        throw new Error(`Sync failed (${response.status}): ${errorData.message || 'Unknown error'}`);
        }

        return await response.json();
    } catch (error) {
        console.error("Prachi :: Sync Error:", error);
    }
};