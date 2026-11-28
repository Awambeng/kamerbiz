import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useAuth } from "./auth/KeycloakProvider";
import { useApi } from "./auth/api";
import ProductPage from "./catalog/ProductPage";
import StorefrontPage from "./storefront/StorefrontPage";
import OrderTrackingPage from "./storefront/OrderTrackingPage";
import OrdersPage from "./orders/OrdersPage";

type Business = {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  logoUrl: string | null;
  phone: string | null;
  whatsapp: string | null;
  email: string | null;
  address: string | null;
  city: string | null;
  region: string | null;
  country: string;
  role: "OWNER" | "MANAGER" | "STAFF";
};

type Profile = {
  name: string;
  slug: string;
  description: string;
  logoUrl: string;
  phone: string;
  whatsapp: string;
  email: string;
  address: string;
  city: string;
  region: string;
  country: string;
};

const emptyProfile: Profile = {
  name: "",
  slug: "",
  description: "",
  logoUrl: "",
  phone: "",
  whatsapp: "",
  email: "",
  address: "",
  city: "",
  region: "",
  country: "Cameroon",
};

const translations = {
  en: {
    eyebrow: "YOUR BUSINESS, READY TO GROW",
    title: "Bring your business online.",
    intro: "Create your KamerBiz business space and keep your essential details in one place.",
    createTitle: "Set up your business",
    businessName: "Business name",
    businessPlaceholder: "Example: Grace Fashion",
    create: "Create business",
    businesses: "Your businesses",
    empty: "Your business space starts here. Add your first business to continue.",
    profile: "Business profile",
    description: "A short description",
    logo: "Logo URL",
    slug: "Store address",
    phone: "Phone number",
    whatsapp: "WhatsApp number",
    email: "Business email",
    address: "Street address",
    city: "City",
    region: "Region",
    country: "Country",
    save: "Save changes",
    saving: "Saving...",
    signIn: "Sign in to KamerBiz",
    signOut: "Sign out",
    loading: "Loading your business space...",
    owner: "Owner",
    manager: "Manager",
    staff: "Staff",
    created: "Your business is ready.",
    saved: "Your changes have been saved.",
    required: "Please enter your business name.",
    error: "Something went wrong. Please try again.",
    storeConflict: "This store address is already in use.",
    validationError: "Please check the information you entered.",
    identityError: "Identity service is not configured. Check the VITE_KEYCLOAK settings in .env.",
  },
  fr: {
    eyebrow: "VOTRE ENTREPRISE, PRÊTE À GRANDIR",
    title: "Mettez votre entreprise en ligne.",
    intro:
      "Créez votre espace KamerBiz et rassemblez les informations essentielles de votre entreprise.",
    createTitle: "Configurez votre entreprise",
    businessName: "Nom de l'entreprise",
    businessPlaceholder: "Exemple : Grace Fashion",
    create: "Créer l'entreprise",
    businesses: "Vos entreprises",
    empty: "Votre espace professionnel commence ici. Ajoutez votre première entreprise.",
    profile: "Profil de l'entreprise",
    description: "Une courte description",
    logo: "Adresse du logo",
    slug: "Adresse de la boutique",
    phone: "Numéro de téléphone",
    whatsapp: "Numéro WhatsApp",
    email: "E-mail professionnel",
    address: "Adresse",
    city: "Ville",
    region: "Région",
    country: "Pays",
    save: "Enregistrer",
    saving: "Enregistrement...",
    signIn: "Se connecter à KamerBiz",
    signOut: "Se déconnecter",
    loading: "Chargement de votre espace...",
    owner: "Propriétaire",
    manager: "Gestionnaire",
    staff: "Personnel",
    created: "Votre entreprise est prête.",
    saved: "Vos modifications ont été enregistrées.",
    required: "Veuillez saisir le nom de votre entreprise.",
    error: "Une erreur est survenue. Veuillez réessayer.",
    storeConflict: "Cette adresse de boutique est déjà utilisée.",
    validationError: "Veuillez vérifier les informations saisies.",
    identityError:
      "Le service d'identité n'est pas configuré. Vérifiez les paramètres VITE_KEYCLOAK dans .env.",
  },
} as const;

type Language = keyof typeof translations;

type ErrorMessages = Pick<
  (typeof translations)[Language],
  "storeConflict" | "validationError" | "error"
>;

async function responseData<T>(response: Response, messages: ErrorMessages): Promise<T> {
  if (!response.ok) {
    if (response.status === 409) {
      throw new Error(messages.storeConflict);
    }
    if (response.status === 400) {
      throw new Error(messages.validationError);
    }
    throw new Error(messages.error);
  }
  return response.json() as Promise<T>;
}

export default function App() {
  if (window.location.pathname.startsWith("/store/")) {
    return <StorefrontPage />;
  }
  if (window.location.pathname.startsWith("/track/")) {
    return <OrderTrackingPage />;
  }
  return <BusinessWorkspace />;
}

function BusinessWorkspace() {
  const auth = useAuth();
  const apiFetch = useApi();
  const [language, setLanguage] = useState<Language>("en");
  const t = translations[language];
  const [businesses, setBusinesses] = useState<Business[]>([]);
  const [selected, setSelected] = useState<Business | null>(null);
  const [profile, setProfile] = useState<Profile>(emptyProfile);
  const [newBusinessName, setNewBusinessName] = useState("");
  const [busy, setBusy] = useState(false);
  const [loadingBusinesses, setLoadingBusinesses] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [activeSection, setActiveSection] = useState<"profile" | "products" | "orders">("profile");

  const loadBusinesses = useCallback(async () => {
    setLoadingBusinesses(true);
    setError("");
    try {
      const response = await apiFetch("/api/v1/businesses");
      const result = await responseData<Business[]>(response, t);
      setBusinesses(result);
      setSelected((current) => result.find((item) => item.id === current?.id) ?? result[0] ?? null);
    } catch {
      setError(t.error);
    } finally {
      setLoadingBusinesses(false);
    }
  }, [apiFetch, t.error]);

  useEffect(() => {
    if (auth.authenticated) {
      void loadBusinesses();
    }
  }, [auth.authenticated, loadBusinesses]);

  useEffect(() => {
    if (!selected) {
      setProfile(emptyProfile);
      return;
    }
    setProfile({
      name: selected.name,
      slug: selected.slug,
      description: selected.description ?? "",
      logoUrl: selected.logoUrl ?? "",
      phone: selected.phone ?? "",
      whatsapp: selected.whatsapp ?? "",
      email: selected.email ?? "",
      address: selected.address ?? "",
      city: selected.city ?? "",
      region: selected.region ?? "",
      country: selected.country,
    });
  }, [selected]);

  async function createBusiness(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!newBusinessName.trim()) {
      setError(t.required);
      return;
    }

    setBusy(true);
    setError("");
    setNotice("");
    try {
      const response = await apiFetch("/api/v1/businesses", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: newBusinessName.trim() }),
      });
      const created = await responseData<Business>(response, t);
      setBusinesses((current) => [...current, created]);
      setSelected(created);
      setNewBusinessName("");
      setNotice(t.created);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : t.error);
    } finally {
      setBusy(false);
    }
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected) return;

    setBusy(true);
    setError("");
    setNotice("");
    try {
      const response = await apiFetch(`/api/v1/businesses/${selected.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(profile),
      });
      const updated = await responseData<Business>(response, t);
      setBusinesses((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      setSelected(updated);
      setNotice(t.saved);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : t.error);
    } finally {
      setBusy(false);
    }
  }

  function updateField(field: keyof Profile, value: string) {
    setProfile((current) => ({ ...current, [field]: value }));
  }

  return (
    <main className="workspace-shell">
      <header className="workspace-header">
        <a className="brand" href="/" aria-label="KamerBiz home">
          <span className="brand-mark">K</span>
          <span>KamerBiz</span>
        </a>
        <div className="header-actions">
          <div className="language-switch" aria-label="Language">
            <button className={language === "en" ? "active" : ""} onClick={() => setLanguage("en")}>
              EN
            </button>
            <button className={language === "fr" ? "active" : ""} onClick={() => setLanguage("fr")}>
              FR
            </button>
          </div>
          {auth.authenticated && (
            <button className="text-button" onClick={auth.signOut}>
              {t.signOut}
            </button>
          )}
        </div>
      </header>

      {!auth.configured && (
        <section className="setup-card">
          <p className="eyebrow">{t.eyebrow}</p>
          <h1>{t.title}</h1>
          <p>{t.identityError}</p>
        </section>
      )}
      {auth.configured && !auth.authenticated && (
        <section className="welcome-layout">
          <div className="welcome-copy">
            <p className="eyebrow">
              <span className="pulse" />
              {t.eyebrow}
            </p>
            <h1>{t.title}</h1>
            <p>{t.intro}</p>
            <button className="primary-button" onClick={auth.signIn}>
              {t.signIn}
              <span aria-hidden="true">↗</span>
            </button>
          </div>
          <div className="welcome-art" aria-hidden="true">
            <div className="art-orbit orbit-one" />
            <div className="art-orbit orbit-two" />
            <div className="art-card">
              <span className="art-mark">K</span>
              <strong>Local roots.</strong>
              <strong>Greater reach.</strong>
              <span className="art-line" />
              <small>STRONGER BUSINESSES</small>
            </div>
            <span className="art-spark spark-one">✳</span>
            <span className="art-spark spark-two">✦</span>
          </div>
        </section>
      )}
      {auth.authenticated && (
        <section className="dashboard">
          <div className="dashboard-heading">
            <div>
              <p className="eyebrow">
                <span className="pulse" />
                {t.eyebrow}
              </p>
              <h1>{t.title}</h1>
              <p>{t.intro}</p>
            </div>
            <div className="user-chip">
              <span className="avatar">{(auth.displayName ?? "K").slice(0, 1).toUpperCase()}</span>
              <span>{auth.displayName ?? "KamerBiz user"}</span>
            </div>
          </div>

          {loadingBusinesses ? (
            <p className="auth-status">{t.loading}</p>
          ) : (
            <div className="business-layout">
              <aside className="business-sidebar">
                <div className="section-heading">
                  <h2>{t.businesses}</h2>
                  <span>{businesses.length}</span>
                </div>
                {businesses.length === 0 ? (
                  <p className="empty-copy">{t.empty}</p>
                ) : (
                  businesses.map((business) => (
                    <button
                      key={business.id}
                      className={
                        selected?.id === business.id
                          ? "business-option selected"
                          : "business-option"
                      }
                      onClick={() => {
                        setSelected(business);
                        setNotice("");
                      }}
                    >
                      <span className="business-avatar">
                        {business.name.slice(0, 1).toUpperCase()}
                      </span>
                      <span className="business-option-copy">
                        <strong>{business.name}</strong>
                        <small>/{business.slug}</small>
                      </span>
                      <span className="option-arrow">›</span>
                    </button>
                  ))
                )}

                <form className="create-card" onSubmit={createBusiness}>
                  <h3>{t.createTitle}</h3>
                  <label htmlFor="business-name">{t.businessName}</label>
                  <input
                    id="business-name"
                    value={newBusinessName}
                    onChange={(event) => setNewBusinessName(event.target.value)}
                    placeholder={t.businessPlaceholder}
                    maxLength={255}
                    required
                  />
                  <button className="primary-button full-width" type="submit" disabled={busy}>
                    {busy ? t.saving : t.create}
                  </button>
                </form>
              </aside>

              <div className="profile-panel">
                {selected ? (
                  <>
                    <div className="product-tabs dashboard-tabs">
                      <button
                        className={activeSection === "profile" ? "active" : ""}
                        onClick={() => setActiveSection("profile")}
                      >
                        {t.profile}
                      </button>
                      <button
                        className={activeSection === "products" ? "active" : ""}
                        onClick={() => setActiveSection("products")}
                      >
                        {language === "fr" ? "Produits" : "Products"}
                      </button>
                      <button
                        className={activeSection === "orders" ? "active" : ""}
                        onClick={() => setActiveSection("orders")}
                      >
                        {language === "fr" ? "Commandes" : "Orders"}
                      </button>
                    </div>
                    {activeSection === "orders" ? (
                      <OrdersPage key={selected.id} businessId={selected.id} language={language} />
                    ) : activeSection === "products" ? (
                      <ProductPage
                        key={selected.id}
                        businessId={selected.id}
                        role={selected.role}
                        language={language}
                      />
                    ) : (
                      <>
                        <div className="profile-heading">
                          <div>
                            <p className="eyebrow">{t.profile}</p>
                            <h2>{selected.name}</h2>
                          </div>
                          <span className="role-pill">
                            {t[selected.role.toLowerCase() as "owner" | "manager" | "staff"]}
                          </span>
                        </div>
                        <form className="profile-form" onSubmit={saveProfile}>
                          <fieldset className="profile-fields" disabled={selected.role === "STAFF"}>
                            <label className="field full-field">
                              <span>{t.businessName}</span>
                              <input
                                required
                                maxLength={255}
                                value={profile.name}
                                onChange={(event) => updateField("name", event.target.value)}
                              />
                            </label>
                            <label className="field full-field">
                              <span>{t.slug}</span>
                              <input
                                maxLength={100}
                                value={profile.slug}
                                onChange={(event) => updateField("slug", event.target.value)}
                              />
                            </label>
                            <a
                              className="store-link"
                              href={`/store/${profile.slug}`}
                              target="_blank"
                              rel="noreferrer"
                            >
                              {language === "fr"
                                ? "Voir la boutique publique"
                                : "View public store"}
                            </a>
                            <label className="field full-field">
                              <span>{t.logo}</span>
                              <input
                                type="url"
                                maxLength={1000}
                                value={profile.logoUrl}
                                onChange={(event) => updateField("logoUrl", event.target.value)}
                              />
                            </label>
                            <label className="field full-field">
                              <span>{t.description}</span>
                              <textarea
                                rows={3}
                                maxLength={2000}
                                value={profile.description}
                                onChange={(event) => updateField("description", event.target.value)}
                              />
                            </label>
                            <label className="field">
                              <span>{t.phone}</span>
                              <input
                                maxLength={32}
                                value={profile.phone}
                                onChange={(event) => updateField("phone", event.target.value)}
                              />
                            </label>
                            <label className="field">
                              <span>{t.whatsapp}</span>
                              <input
                                maxLength={32}
                                value={profile.whatsapp}
                                onChange={(event) => updateField("whatsapp", event.target.value)}
                              />
                            </label>
                            <label className="field full-field">
                              <span>{t.email}</span>
                              <input
                                type="email"
                                maxLength={320}
                                value={profile.email}
                                onChange={(event) => updateField("email", event.target.value)}
                              />
                            </label>
                            <label className="field full-field">
                              <span>{t.address}</span>
                              <input
                                maxLength={500}
                                value={profile.address}
                                onChange={(event) => updateField("address", event.target.value)}
                              />
                            </label>
                            <label className="field">
                              <span>{t.city}</span>
                              <input
                                maxLength={120}
                                value={profile.city}
                                onChange={(event) => updateField("city", event.target.value)}
                              />
                            </label>
                            <label className="field">
                              <span>{t.region}</span>
                              <input
                                maxLength={120}
                                value={profile.region}
                                onChange={(event) => updateField("region", event.target.value)}
                              />
                            </label>
                            <label className="field full-field">
                              <span>{t.country}</span>
                              <input
                                maxLength={80}
                                value={profile.country}
                                onChange={(event) => updateField("country", event.target.value)}
                              />
                            </label>
                            <div className="form-footer">
                              <span>{notice}</span>
                              <button className="primary-button" type="submit" disabled={busy}>
                                {busy ? t.saving : t.save}
                              </button>
                            </div>
                          </fieldset>
                        </form>
                      </>
                    )}
                  </>
                ) : (
                  <div className="empty-state">
                    <div className="empty-icon">✳</div>
                    <h2>{t.createTitle}</h2>
                    <p>{t.empty}</p>
                  </div>
                )}
              </div>
            </div>
          )}
          {error && (
            <p className="notice error-notice" role="alert">
              {error}
            </p>
          )}
          {notice && (
            <p className="notice success-notice" role="status">
              {notice}
            </p>
          )}
        </section>
      )}

      <footer className="workspace-footer">
        <span>© {new Date().getFullYear()} KamerBiz</span>
        <span>Stronger businesses. A stronger Cameroon.</span>
      </footer>
    </main>
  );
}
