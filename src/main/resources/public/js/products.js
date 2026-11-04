document.addEventListener("DOMContentLoaded", () => {
  const api = window.StockAPI;
  const form = document.getElementById("product-form");
  const error = document.getElementById("product-error");
  const success = document.getElementById("product-success");
  const rows = document.getElementById("product-rows");
  const dialog = document.getElementById("product-dialog");
  const details = document.getElementById("product-details");
  let searchTimer;
  let editingId = null;
  let products = [];

  function setEditing(product) {
    editingId = product?.id ?? null;
    form.elements.name.value = product?.name ?? "";
    form.elements.sku.value = product?.sku ?? "";
    form.elements.unitPrice.value = product?.unitPrice ?? "";
    form.elements.quantity.value = product?.quantity ?? 0;
    form.elements.lowStockThreshold.value = product?.lowStockThreshold ?? 5;
    form.elements.supplierId.value = product?.supplierId ?? "";
    form.elements.description.value = product?.description ?? "";
    form.elements.quantity.readOnly = Boolean(product);
    document.getElementById("quantity-help").textContent = product
      ? "Stock changes are recorded from the Inventory page."
      : "Set the opening stock. Later adjustments are recorded in Inventory.";
    document.getElementById("product-form-title").textContent = product ? `Edit ${product.name}` : "Add a product";
    document.getElementById("save-product").textContent = product ? "Save changes" : "Add product";
    document.getElementById("cancel-edit").hidden = !product;
    if (product) form.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  function status(product) {
    if (product.quantity === 0) return '<span class="badge critical">Out of stock</span>';
    if (product.quantity <= product.lowStockThreshold) return '<span class="badge warning">Low stock</span>';
    return '<span class="badge good">In stock</span>';
  }

  function renderProducts() {
    const filter = document.getElementById("product-filter").value;
    const sort = document.getElementById("product-sort").value;
    const visible = products.filter((product) => {
      if (filter === "low") return product.quantity > 0 && product.quantity <= product.lowStockThreshold;
      if (filter === "out") return product.quantity === 0;
      if (filter === "available") return product.quantity > 0;
      return true;
    });
    visible.sort((a, b) => {
      if (sort === "quantity") return a.quantity - b.quantity || a.name.localeCompare(b.name);
      if (sort === "price") return Number(a.unitPrice) - Number(b.unitPrice) || a.name.localeCompare(b.name);
      return String(a[sort] || "").localeCompare(String(b[sort] || ""), undefined, { numeric: true, sensitivity: "base" });
    });
    document.getElementById("product-total").textContent = visible.length;
    rows.innerHTML = visible.length ? visible.map((product) => `
      <tr>
        <td><button class="table-link product-name" type="button" data-view="${product.id}">${api.esc(product.name)}</button>${product.description ? `<span class="subtext">${api.esc(product.description)}</span>` : ""}</td>
        <td>${api.esc(product.sku)}</td><td class="numeric">${api.money(product.unitPrice)}</td>
        <td class="numeric">${product.quantity}</td><td class="numeric">${product.lowStockThreshold}</td>
        <td>${status(product)}</td><td>${api.esc(product.supplierName || "—")}</td>
        <td><div class="action-group"><button class="button secondary small" type="button" data-edit="${product.id}">Edit</button><button class="button danger small" type="button" data-delete="${product.id}">Delete</button></div></td>
      </tr>`).join("") : '<tr><td colspan="8" class="empty">No products match this view.</td></tr>';
  }

  async function loadProducts() {
    try {
      api.clearError(error);
      products = await api.get(`/api/products${document.getElementById("product-search").value.trim()
        ? `?q=${encodeURIComponent(document.getElementById("product-search").value.trim())}` : ""}`);
      renderProducts();
    } catch (requestError) {
      api.showError(error, requestError);
      rows.innerHTML = '<tr><td colspan="8" class="empty">Products could not be loaded. Please try again.</td></tr>';
    }
  }

  async function loadSuppliers() {
    try {
      const suppliers = await api.get("/api/suppliers");
      document.getElementById("supplier-select").insertAdjacentHTML("beforeend",
        suppliers.map((supplier) => `<option value="${supplier.id}">${api.esc(supplier.name)}</option>`).join(""));
    } catch (requestError) {
      api.showError(error, requestError);
    }
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    api.clearError(error);
    api.clearError(success);
    const data = new FormData(form);
    const supplierValue = data.get("supplierId");
    const button = form.querySelector('[type="submit"]');
    const payload = {
      name: data.get("name"),
      sku: data.get("sku"),
      unitPrice: Number(data.get("unitPrice")),
      quantity: Number(form.elements.quantity.value),
      lowStockThreshold: Number(data.get("lowStockThreshold")),
      supplierId: supplierValue ? Number(supplierValue) : null,
      description: data.get("description") || null
    };
    button.disabled = true;
    try {
      if (editingId) {
        await api.put(`/api/products/${editingId}`, payload);
        success.textContent = "Product changes saved.";
      } else {
        await api.post("/api/products", payload);
        success.textContent = "Product added.";
      }
      success.hidden = false;
      setEditing(null);
      form.reset();
      form.elements.lowStockThreshold.value = "5";
      form.elements.quantity.value = "0";
      await loadProducts();
    } catch (requestError) {
      api.showError(error, requestError);
    } finally {
      button.disabled = false;
    }
  });

  rows.addEventListener("click", async (event) => {
    const viewButton = event.target.closest("[data-view]");
    const editButton = event.target.closest("[data-edit]");
    const deleteButton = event.target.closest("[data-delete]");
    try {
      if (viewButton) {
        const product = await api.get(`/api/products/${viewButton.dataset.view}`);
        details.innerHTML = `<p class="eyebrow">PRODUCT DETAILS</p><h2>${api.esc(product.name)}</h2>
          <dl class="detail-list"><dt>SKU</dt><dd>${api.esc(product.sku)}</dd>
          <dt>Unit price</dt><dd>${api.money(product.unitPrice)}</dd>
          <dt>Available stock</dt><dd>${product.quantity}</dd>
          <dt>Reorder level</dt><dd>${product.lowStockThreshold}</dd>
          <dt>Supplier</dt><dd>${api.esc(product.supplierName || "Not assigned")}</dd>
          <dt>Description</dt><dd>${api.esc(product.description || "No description")}</dd>
          <dt>Added</dt><dd>${api.esc(api.date(product.createdAt))}</dd>
          <dt>Last updated</dt><dd>${api.esc(api.date(product.updatedAt))}</dd></dl>`;
        dialog.showModal();
      } else if (editButton) {
        const product = await api.get(`/api/products/${editButton.dataset.edit}`);
        setEditing(product);
      } else if (deleteButton) {
        const product = products.find((item) => item.id === Number(deleteButton.dataset.delete));
        if (!window.confirm(`Delete "${product?.name || "this product"}"? Products with sale or stock history cannot be deleted.`)) return;
        await api.delete(`/api/products/${deleteButton.dataset.delete}`);
        success.textContent = "Product deleted.";
        success.hidden = false;
        await loadProducts();
      }
    } catch (requestError) {
      api.showError(error, requestError);
    }
  });

  document.getElementById("cancel-edit").addEventListener("click", () => {
    form.reset();
    setEditing(null);
    form.elements.lowStockThreshold.value = "5";
    form.elements.quantity.value = "0";
    api.clearError(error);
  });
  document.getElementById("product-search").addEventListener("input", () => {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(loadProducts, 250);
  });
  document.getElementById("product-filter").addEventListener("change", renderProducts);
  document.getElementById("product-sort").addEventListener("change", renderProducts);

  setEditing(null);
  loadSuppliers();
  loadProducts();
});
