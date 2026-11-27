import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import { KeycloakProvider } from "./auth/KeycloakProvider";
import "./styles.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <KeycloakProvider>
      <App />
    </KeycloakProvider>
  </React.StrictMode>,
);
