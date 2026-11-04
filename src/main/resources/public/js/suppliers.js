document.addEventListener("DOMContentLoaded", () => {
  const api = window.StockAPI;
  const form = document.getElementById("supplier-form");
  const error = document.getElementById("supplier-error");
  const success = document.getElementById("supplier-success");
  const rows = document.getElementById("supplier-rows");
  let suppliers = [];
  let editingId = null;

  function resetForm() {
    editingId = null;
    form.reset();
    document.getElementById("supplier-form-title").textContent = "Add a supplier";
    document.getElementById("supplier-save").textContent = "Add supplier";
    document.getElementById("supplier-cancel").hidden = true;
  }

  async function loadSuppliers() {
    try {
      api.clearError(error);
      suppliers = await api.get("/api/suppliers");
      document.getElementById("supplier-count").textContent = suppliers.length;
      rows.innerHTML = suppliers.length ? suppliers.map((supplier) => `<tr>
        <td><strong>${api.esc(supplier.name)}</strong></td>
        <td>${api.esc(supplier.contactName || "—")}</td>
        <td>${supplier.email ? `<a class="table-link" href="mailto:${api.esc(supplier.email)}">${api.esc(supplier.email)}</a>` : "—"}</td>
        <td>${supplier.phone ? `<a class="table-link" href="tel:${api.esc(supplier.phone)}">${api.esc(supplier.phone)}</a>` : "—"}</td>
        <td><button class="button secondary small" type="button" data-edit="${supplier.id}">Edit</button></td>
      </tr>`).join("") : '<tr><td colspan="5" class="empty">No suppliers yet. Add a supplier above or continue without one.</td></tr>';
    } catch (requestError) {
      api.showError(error, requestError);
      rows.innerHTML = '<tr><td colspan="5" class="empty">Suppliers could not be loaded. Please try again.</td></tr>';
    }
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    api.clearError(error);
    api.clearError(success);
    const data = new FormData(form);
    const button = form.querySelector('[type="submit"]');
    const supplier = {
      name: data.get("name"),
      contactName: data.get("contactName") || null,
      email: data.get("email") || null,
      phone: data.get("phone") || null
    };
    button.disabled = true;
    try {
      if (editingId) {
        await api.put(`/api/suppliers/${editingId}`, supplier);
        success.textContent = "Supplier details updated.";
      } else {
        await api.post("/api/suppliers", supplier);
        success.textContent = "Supplier added.";
      }
      success.hidden = false;
      resetForm();
      await loadSuppliers();
    } catch (requestError) {
      api.showError(error, requestError);
    } finally {
      button.disabled = false;
    }
  });

  rows.addEventListener("click", (event) => {
    const button = event.target.closest("[data-edit]");
    if (!button) return;
    const supplier = suppliers.find((item) => item.id === Number(button.dataset.edit));
    if (!supplier) return;
    editingId = supplier.id;
    form.elements.name.value = supplier.name;
    form.elements.contactName.value = supplier.contactName || "";
    form.elements.email.value = supplier.email || "";
    form.elements.phone.value = supplier.phone || "";
    document.getElementById("supplier-form-title").textContent = `Edit ${supplier.name}`;
    document.getElementById("supplier-save").textContent = "Save changes";
    document.getElementById("supplier-cancel").hidden = false;
    form.scrollIntoView({ behavior: "smooth", block: "start" });
    form.elements.name.focus();
  });

  document.getElementById("supplier-cancel").addEventListener("click", resetForm);
  loadSuppliers();
});
