import Keycloak from "keycloak-js";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";

type AuthContextValue = {
  configured: boolean;
  authenticated: boolean;
  loading: boolean;
  displayName: string | null;
  error: string | null;
  signIn: () => void;
  signOut: () => void;
  getAccessToken: () => Promise<string>;
};

const AuthContext = createContext<AuthContextValue | null>(null);
const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL;
const keycloakRealm = import.meta.env.VITE_KEYCLOAK_REALM;
const keycloakClientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID;
const configured = Boolean(keycloakUrl && keycloakRealm && keycloakClientId);
const keycloak = configured
  ? new Keycloak({ url: keycloakUrl, realm: keycloakRealm, clientId: keycloakClientId })
  : null;
let initialization: Promise<boolean> | undefined;

export function KeycloakProvider({ children }: { children: ReactNode }) {
  const [loading, setLoading] = useState(configured);
  const [authenticated, setAuthenticated] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!keycloak) return;
    let active = true;

    initialization ??= keycloak.init({
      onLoad: "check-sso",
      pkceMethod: "S256",
      checkLoginIframe: false,
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    });

    initialization
      .then((isAuthenticated) => {
        if (active) {
          setAuthenticated(isAuthenticated);
          setLoading(false);
        }
      })
      .catch(() => {
        if (active) {
          setError("Unable to connect to the identity service. Please try again.");
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const signIn = useCallback(() => {
    void keycloak?.login({ redirectUri: window.location.origin });
  }, []);

  const signOut = useCallback(() => {
    void keycloak?.logout({ redirectUri: window.location.origin });
  }, []);

  const getAccessToken = useCallback(async () => {
    if (!keycloak?.authenticated) throw new Error("Authentication is required.");
    await keycloak.updateToken(30);
    if (!keycloak.token) throw new Error("The identity service did not return an access token.");
    return keycloak.token;
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      configured,
      authenticated,
      loading,
      displayName: keycloak?.tokenParsed?.name ?? keycloak?.tokenParsed?.preferred_username ?? null,
      error,
      signIn,
      signOut,
      getAccessToken,
    }),
    [authenticated, error, getAccessToken, loading, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside KeycloakProvider.");
  return context;
}
