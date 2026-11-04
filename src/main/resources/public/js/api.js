(() => {
  async function request(path, options = {}) {
    let response;
    try {
      response = await fetch(path, {
        ...options,
        headers: {
          ...(options.body ? { "Content-Type": "application/json" } : {}),
          ...options.headers
        }
      });
    } catch (error) {
      if (error.name === "AbortError") throw new Error("The request was cancelled.");
      throw new Error("Cannot reach Stockroom. Check that the server is running and try again.");
    }
    if (response.status === 204) return null;
    let payload;
    try {
      payload = await response.json();
    } catch {
      throw new Error(response.ok ? "The server returned an unreadable response." : `Request failed (${response.status}).`);
    }
    if (!response.ok) {
      const error = new Error(payload.error || `Request failed (${response.status}).`);
      error.status = response.status;
      throw error;
    }
    return payload;
  }

  const json = (method, body) => ({ method, body: JSON.stringify(body) });
  const esc = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  }[character]));

  window.StockAPI = {
    get: (path) => request(path),
    post: (path, body) => request(path, json("POST", body)),
    put: (path, body) => request(path, json("PUT", body)),
    delete: (path) => request(path, { method: "DELETE" }),
    money: (value) => new Intl.NumberFormat(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(value || 0)),
    date: (value) => {
      if (!value) return "—";
      const normalized = String(value).includes(" ") && !String(value).includes("T")
        ? String(value).replace(" ", "T") + "Z" : value;
      const parsed = new Date(normalized);
      return Number.isNaN(parsed.getTime()) ? String(value) : parsed.toLocaleString();
    },
    esc,
    showError(element, error) {
      element.textContent = error.message || "Something went wrong.";
      element.hidden = false;
    },
    clearError(element) {
      element.hidden = true;
      element.textContent = "";
    }
  };

  document.addEventListener("DOMContentLoaded", () => {
    const button = document.querySelector(".menu-toggle");
    const navigation = document.querySelector(".navigation");
    button?.addEventListener("click", () => {
      const open = button.getAttribute("aria-expanded") !== "true";
      button.setAttribute("aria-expanded", String(open));
      navigation?.classList.toggle("open", open);
    });
  });
})();