import { useEffect, useState } from "react";

const API_URL = import.meta.env.VITE_API_URL || "";

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  const method = (options.method || "GET").toUpperCase();
  if (options.body) headers.set("Content-Type", "application/json");
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const csrfResponse = await fetch(`${API_URL}/api/auth/csrf`, { credentials: "include" });
    if (!csrfResponse.ok) throw new Error("Could not start a secure API session.");
    const csrf = await csrfResponse.json();
    headers.set("X-XSRF-TOKEN", csrf.token);
  }

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers,
    credentials: "include",
  });
  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const message = data?.detail || data?.message || data?.error || `Request failed (${response.status}).`;
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  return data;
}

function App() {
  const [jobs, setJobs] = useState([]);
  const [user, setUser] = useState(null);
  const [page, setPage] = useState("home");
  const [selectedJob, setSelectedJob] = useState(null);
  const [query, setQuery] = useState("");
  const [applications, setApplications] = useState([]);
  const [adminJobs, setAdminJobs] = useState([]);
  const [editingJob, setEditingJob] = useState(null);
  const [pendingDestination, setPendingDestination] = useState(null);
  const [pendingJobId, setPendingJobId] = useState(null);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  async function loadJobs(search = "") {
    const data = await request(`/api/jobs${search ? `?q=${encodeURIComponent(search)}` : ""}`);
    setJobs(data);
  }

  useEffect(() => {
    Promise.all([
      loadJobs(),
      request("/api/auth/me").then(setUser).catch((requestError) => {
        if (requestError.status !== 401) throw requestError;
        setUser(null);
      }),
    ])
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoading(false));
  }, []);

  async function navigate(nextPage, authenticatedUser = user) {
    if (["post", "applications", "admin"].includes(nextPage) && !authenticatedUser) {
      setPendingDestination(nextPage);
      nextPage = "login";
    }
    if (nextPage === "admin" && authenticatedUser?.role !== "ADMIN") {
      setError("Admin access is required to manage job listings.");
      return;
    }
    setError("");
    setNotice("");
    setPage(nextPage);
    if (nextPage !== "login" && nextPage !== "register") setSelectedJob(null);
    if (nextPage === "applications") {
      try {
        setApplications(await request("/api/applications"));
      } catch (requestError) {
        setError(requestError.message);
      }
    }
    if (nextPage === "admin") {
      try {
        setAdminJobs(await request("/api/admin/jobs"));
      } catch (requestError) {
        setError(requestError.message);
      }
    }
  }

  async function search(event) {
    event.preventDefault();
    setError("");
    try {
      await loadJobs(query.trim());
      setPage("home");
      setSelectedJob(null);
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function authenticate(event, mode) {
    event.preventDefault();
    setError("");
    const form = new FormData(event.currentTarget);
    const payload = Object.fromEntries(form.entries());
    try {
      if (mode === "register") {
        await request("/api/auth/register", { method: "POST", body: JSON.stringify(payload) });
      }
      const account = await request("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email: payload.email, password: payload.password }),
      });
      setUser(account);
      setNotice(mode === "register" ? "Your account is ready. Welcome to JobBridge!" : "Welcome back!");
      const destination = pendingDestination || "home";
      const jobToApply = pendingJobId;
      setPendingDestination(null);
      setPendingJobId(null);
      await navigate(destination, account);
      if (jobToApply) {
        const job = jobs.find((item) => item.id === jobToApply);
        if (job) await apply(job);
      }
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function logout() {
    setError("");
    try {
      await request("/api/auth/logout", { method: "POST" });
      setUser(null);
      setNotice("You have been logged out.");
      setPage("home");
      setSelectedJob(null);
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function apply(job) {
    setError("");
    try {
      await request(`/api/jobs/${job.id}/applications`, { method: "POST" });
      setNotice("Application submitted successfully.");
    } catch (requestError) {
      if (requestError.status === 401) {
        setPendingJobId(job.id);
        setPendingDestination("home");
        setPage("login");
        setError("Please log in to apply. We'll return you to this job after login.");
      } else {
        setError(requestError.message);
      }
    }
  }

  async function publishJob(event) {
    event.preventDefault();
    setError("");
    const payload = Object.fromEntries(new FormData(event.currentTarget).entries());
    try {
      const job = await request("/api/jobs", { method: "POST", body: JSON.stringify(payload) });
      setJobs((current) => [job, ...current]);
      setSelectedJob(job);
      setPage("home");
      setNotice("Job published successfully.");
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function updateAdminJob(event, jobId) {
    event.preventDefault();
    setError("");
    const payload = Object.fromEntries(new FormData(event.currentTarget).entries());
    try {
      const updated = await request(`/api/admin/jobs/${jobId}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      setAdminJobs((current) => current.map((job) => job.id === jobId ? updated : job));
      setJobs((current) => current.map((job) => job.id === jobId ? updated : job));
      setEditingJob(null);
      setNotice("Job listing updated.");
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  async function setJobPublished(job) {
    const published = !job.published;
    if (!published && !window.confirm(`Remove "${job.title}" from public search?`)) return;
    setError("");
    try {
      const updated = await request(`/api/admin/jobs/${job.id}/publication`, {
        method: "PATCH",
        body: JSON.stringify({ published }),
      });
      setAdminJobs((current) => current.map((item) => item.id === job.id ? updated : item));
      if (published) {
        setJobs((current) => [updated, ...current.filter((item) => item.id !== updated.id)]);
      } else {
        setJobs((current) => current.filter((item) => item.id !== updated.id));
      }
      setNotice(published ? "Job listing restored." : "Job listing removed from public search.");
    } catch (requestError) {
      setError(requestError.message);
    }
  }

  function openJob(job) {
    setSelectedJob(job);
    setPage("home");
    setNotice("");
  }

  return (
    <>
      <header className="nav">
        <a className="brand" href="#" onClick={(event) => { event.preventDefault(); navigate("home"); }}>
          <span className="brand-mark">J</span> jobbridge
        </a>
        <nav>
          <button onClick={() => navigate("home")}>Find jobs</button>
          <button onClick={() => navigate("post")}>For employers</button>
          {user && <button onClick={() => navigate("applications")}>My applications</button>}
          {user?.role === "ADMIN" && <button onClick={() => navigate("admin")}>Admin</button>}
        </nav>
        <div className="nav-actions">
          {user ? (
            <>
              <span className="user-email">{user.fullName}</span>
              <button className="btn btn-light" onClick={logout}>Log out</button>
            </>
          ) : (
            <>
              <button className="btn btn-light" onClick={() => navigate("login")}>Log in</button>
              <button className="btn btn-primary" onClick={() => navigate("register")}>Get started</button>
            </>
          )}
        </div>
      </header>

      {error && <div className="flash error" role="alert">{error}<button onClick={() => setError("")}>×</button></div>}
      {notice && <div className="flash success" role="status">{notice}<button onClick={() => setNotice("")}>×</button></div>}

      {page === "home" && (
        <main>
          {!selectedJob ? (
            <>
              <section className="hero">
                <div className="hero-copy">
                  <div className="eyebrow"><span className="live-dot" /> YOUR NEXT CHAPTER STARTS HERE</div>
                  <h1>Find work that<br /><span>moves you forward.</span></h1>
                  <p className="hero-text">Discover opportunities that match your ambition. Meet teams building what comes next.</p>
                  <form className="search-box" onSubmit={search}>
                    <span>⌕</span>
                    <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Job title, company or location" aria-label="Search jobs" />
                    <button className="btn btn-primary">Search jobs <span>→</span></button>
                  </form>
                  <div className="hero-note">✓ &nbsp; Curated opportunities · A clearer way to get hired</div>
                </div>
                <div className="hero-art" aria-hidden="true">
                  <div className="art-orbit orbit-one" /><div className="art-orbit orbit-two" />
                  <div className="art-card"><div className="art-top"><span className="art-icon">✳</span><span className="art-status">NEW OPPORTUNITY</span></div><div className="art-label">Your next move</div><div className="art-role">Something<br />remarkable.</div><div className="art-line" /><div className="art-bottom"><span>Skills meet possibility</span><span>↗</span></div></div>
                  <div className="floating-chip chip-one">✦ &nbsp; Your potential, connected</div>
                  <div className="floating-chip chip-two"><span className="live-dot" /> Opportunities await</div>
                </div>
              </section>
              <section className="stats"><div><strong>{jobs.length}</strong><span>Open opportunities</span></div><div><strong>One place</strong><span>For your next move</span></div><div><strong>Built for you</strong><span>Simple. Focused. Human.</span></div></section>
              <section className="jobs-section">
                <div className="section-heading"><div><div className="eyebrow">EXPLORE OPPORTUNITIES</div><h2>{query ? "Search results" : "Roles worth a closer look"}</h2><p>Find a role where your skills can make a difference.</p></div></div>
                {loading ? <p>Loading opportunities…</p> : <JobGrid jobs={jobs} onOpen={openJob} />}
              </section>
              <section className="cta"><div><div className="eyebrow">READY FOR WHAT'S NEXT?</div><h2>Your next opportunity<br />could start today.</h2></div><button className="btn btn-white" onClick={() => navigate(user ? "applications" : "register")}>{user ? "View your applications" : "Create your free profile"} <span>→</span></button></section>
            </>
          ) : (
            <main className="detail-wrap">
              <button className="text-link back-button" onClick={() => setSelectedJob(null)}>← Back to opportunities</button>
              <article className="detail-card">
                <div className="company-icon large">{selectedJob.company.charAt(0)}</div>
                <div className="eyebrow">JOB OPPORTUNITY</div><h1>{selectedJob.title}</h1><p className="detail-company">{selectedJob.company}</p>
                <div className="job-meta detail-meta"><span>⌖ {selectedJob.location}</span><span>◷ {selectedJob.jobType}</span><span>₹ {selectedJob.salary}</span></div>
                <hr /><h2>About this role</h2><p className="description">{selectedJob.description}</p>
                <button className="btn btn-primary" onClick={() => apply(selectedJob)}>{user ? "Apply for this role" : "Log in to apply"} <span>→</span></button>
              </article>
            </main>
          )}
        </main>
      )}

      {(page === "login" || page === "register") && (
        <main className="auth-wrap"><section className="auth-card">
          <div className="eyebrow">{page === "login" ? "WELCOME BACK" : "MAKE YOUR NEXT MOVE"}</div>
          <h1>{page === "login" ? "Good to see you." : "Let's get you started."}</h1>
          <p>{page === "login" ? "Log in to pick up where your next chapter begins." : "Create your profile and start exploring opportunities."}</p>
          <form className="form-stack" onSubmit={(event) => authenticate(event, page)}>
            {page === "register" && <label>Full name<input name="fullName" required maxLength="80" placeholder="Your full name" /></label>}
            <label>Email address<input name="email" type="email" required maxLength="160" placeholder="you@example.com" /></label>
            <label>Password<input name="password" type="password" required minLength="8" maxLength="72" autoComplete={page === "login" ? "current-password" : "new-password"} placeholder="At least 8 characters" /></label>
            <button className="btn btn-primary full">{page === "login" ? "Log in" : "Create account"} <span>→</span></button>
          </form>
          <p className="auth-switch">{page === "login" ? "New to JobBridge?" : "Already have an account?"} <button onClick={() => navigate(page === "login" ? "register" : "login")}>{page === "login" ? "Create an account" : "Log in"}</button></p>
        </section></main>
      )}

      {page === "post" && (
        <main className="auth-wrap"><section className="auth-card wide">
          <div className="eyebrow">FOR EMPLOYERS</div><h1>Find your next great hire.</h1><p>Share an opportunity with people ready to make a difference.</p>
          <form className="form-stack" onSubmit={publishJob}>
            <label>Job title<input name="title" required maxLength="100" placeholder="e.g. Software Engineer" /></label>
            <label>Company name<input name="company" required maxLength="100" placeholder="Your company" /></label>
            <label>Location<input name="location" required maxLength="100" placeholder="e.g. Hyderabad / Remote" /></label>
            <label>Employment type<select name="jobType" defaultValue="Full-time"><option>Full-time</option><option>Part-time</option><option>Internship</option><option>Contract</option></select></label>
            <label>Salary range<input name="salary" required maxLength="30" placeholder="e.g. ₹5–8 LPA" /></label>
            <label>Job description<textarea name="description" required maxLength="3000" rows="5" placeholder="Describe the role, responsibilities and skills…" /></label>
            <button className="btn btn-primary full">Publish opportunity <span>→</span></button>
          </form>
        </section></main>
      )}

      {page === "applications" && (
        <main className="jobs-section application-page"><div className="eyebrow">YOUR CAREER JOURNEY</div><h1>My applications</h1><p>Keep track of the opportunities you've applied for.</p>
          {applications.length ? <div className="job-grid">{applications.map((application) => <article className="job-card" key={`${application.jobId}-${application.appliedAt}`}><div className="company-icon">{application.company.charAt(0)}</div><h3>{application.title}</h3><div className="company-name">{application.company}</div><div className="job-meta"><span>{application.location}</span><span>{new Date(application.appliedAt).toLocaleDateString()}</span></div><span className="job-type">Application submitted</span></article>)}</div> : <div className="empty-state"><h3>Your next step starts here.</h3><p>You haven't applied to any jobs yet.</p><button className="btn btn-primary" onClick={() => navigate("home")}>Explore jobs</button></div>}
        </main>
      )}

      {page === "admin" && user?.role === "ADMIN" && (
        <main className="jobs-section admin-page">
          <div className="eyebrow">ADMINISTRATION</div>
          <h1>Manage job listings</h1>
          <p>Review, update, and remove listings from public search. Removed listings can be restored.</p>
          <div className="admin-list">
            {adminJobs.map((job) => (
              <article className="admin-job" key={job.id}>
                {editingJob === job.id ? (
                  <form className="form-stack" onSubmit={(event) => updateAdminJob(event, job.id)}>
                    <label>Job title<input name="title" required maxLength="100" defaultValue={job.title} /></label>
                    <label>Company name<input name="company" required maxLength="100" defaultValue={job.company} /></label>
                    <label>Location<input name="location" required maxLength="100" defaultValue={job.location} /></label>
                    <label>Employment type<select name="jobType" defaultValue={job.jobType}><option>Full-time</option><option>Part-time</option><option>Internship</option><option>Contract</option></select></label>
                    <label>Salary range<input name="salary" required maxLength="30" defaultValue={job.salary} /></label>
                    <label>Job description<textarea name="description" required maxLength="3000" rows="4" defaultValue={job.description} /></label>
                    <div className="admin-actions"><button className="btn btn-primary" type="submit">Save changes</button><button className="btn btn-light" type="button" onClick={() => setEditingJob(null)}>Cancel</button></div>
                  </form>
                ) : (
                  <>
                    <div className="admin-job-heading"><div><h2>{job.title}</h2><p>{job.company} · {job.location}</p></div><span className={`job-type ${job.published ? "" : "unpublished"}`}>{job.published ? "Published" : "Removed"}</span></div>
                    <div className="admin-actions"><button className="btn btn-light" onClick={() => setEditingJob(job.id)}>Edit listing</button><button className={`btn ${job.published ? "btn-danger" : "btn-primary"}`} onClick={() => setJobPublished(job)}>{job.published ? "Remove listing" : "Restore listing"}</button></div>
                  </>
                )}
              </article>
            ))}
            {!adminJobs.length && <div className="empty-state"><h3>No job listings yet</h3><p>Job postings will appear here for review.</p></div>}
          </div>
        </main>
      )}

      <footer className="footer"><a className="brand" href="#" onClick={(event) => { event.preventDefault(); navigate("home"); }}><span className="brand-mark">J</span> jobbridge</a><span>Connecting ambition with opportunity.</span><span>© 2026 JobBridge</span></footer>
    </>
  );
}

function JobGrid({ jobs, onOpen }) {
  if (!jobs.length) return <div className="empty-state"><h3>No roles found just yet</h3><p>Try another keyword or check back soon.</p></div>;
  return <div className="job-grid">{jobs.map((job) => <article className="job-card" key={job.id}>
    <div className="job-card-top"><div className="company-icon">{job.company.charAt(0)}</div><span className="job-type">{job.jobType}</span></div>
    <h3><button className="job-title" onClick={() => onOpen(job)}>{job.title}</button></h3><div className="company-name">{job.company}</div>
    <div className="job-meta"><span>⌖ {job.location}</span><span>◷ {job.salary}</span></div>
    <div className="card-bottom"><span className="match-label">A new possibility</span><button className="arrow-link" aria-label={`View ${job.title}`} onClick={() => onOpen(job)}>↗</button></div>
  </article>)}</div>;
}

export default App;
