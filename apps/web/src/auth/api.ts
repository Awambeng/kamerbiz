import { useAuth } from "./KeycloakProvider";

export function useApi() {
  const { getAccessToken } = useAuth();

  return async function apiFetch(input: RequestInfo | URL, init: RequestInit = {}) {
    const token = await getAccessToken();
    const headers = new Headers(init.headers);
    headers.set("Authorization", "Bearer " + token);
    return fetch(input, { ...init, headers });
  };
}
