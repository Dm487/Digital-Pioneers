document.addEventListener("DOMContentLoaded", () => {
  const api = window.StockAPI;
  const form = document.getElementById("sale-form");
  const lines = document.getElementById("sale-lines");
  const error = document.getElementById("sales-error");
  const success = document.getElementById("sales-success");
  const submitButton = form.querySelector('[type="submit"]');
  let products = [];

  function addLine() {
    const row = document.createElement("div");
    row.className = "sale-line";
    row.innerHTML = `<label>Product<select class="sale-product" required><option value="">Choose a product</option>${products.map((product) =>
      `<option value="${product.id}" ${product.quantity < 1 ? "disabled" : ""}>${api.esc(product.name)} (${api.esc(product.sku)}) — ${product.quantity} in stock · ${api.money(product.unitPrice)}</option>`).join("")}</select></label>
      <label>Quantity<input class="sale-quantity" type="number" min="1" step="1" value="1" required></label>
      <button class="button danger small remove-line" type="button" aria-label="Remove item">Remove</button>`;
    lines.append(row);
    row.querySelectorAll("select, input").forEach((input) => input.addEventListener("input", updateTotal));
    updateTotal();
  }

  function updateTotal() {
    let total = 0;
    for (const row of lines.querySelectorAll(".sale-line")) {
      const product = products.find((item) => item.id === Number(row.querySelector(".sale-product").value));
      const quantity = Number(row.querySelector(".sale-quantity").value);
      if (product && Number.isInteger(quantity) && quantity > 0) total += Number(product.unitPrice) * quantity;
    }
    document.getElementById("sale-total").textContent = api.money(total);
  }

  async function loadSales() {
    const table = document.getElementById("sales-rows");
    try {
      const sales = await api.get("/api/sales");
      table.innerHTML = sales.length ? sales.map((sale) => `<tr id="sale-${sale.id}">
        <td><button class="table-link" type="button" data-details="${sale.id}">Sale #${sale.id}</button></td>
        <td>${api.esc(api.date(sale.createdAt))}</td>
        <td>${sale.items.map((item) => `${api.esc(item.productName)} × ${item.quantity}`).join("<br>")}</td>
        <td class="numeric">${api.money(sale.total)}</td>
        <td><button class="button secondary small" type="button" data-details="${sale.id}">Details</button></td></tr>`).join("")
        : '<tr><td colspan="5" class="empty">No sales recorded yet. Completed sales will appear here.</td></tr>';
    } catch (requestError) {
      api.showError(error, requestError);
      table.innerHTML = '<tr><td colspan="5" class="empty">Sales could not be loaded. Please try again.</td></tr>';
    }
  }

  document.getElementById("add-sale-line").addEventListener("click", addLine);
  lines.addEventListener("click", (event) => {
    if (event.target.closest(".remove-line")) {
      event.target.closest(".sale-line").remove();
      updateTotal();
    }
  });

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    api.clearError(error);
    api.clearError(success);
    const items = [...lines.querySelectorAll(".sale-line")].map((row) => ({
      productId: Number(row.querySelector(".sale-product").value),
      quantity: Number(row.querySelector(".sale-quantity").value)
    }));
    if (!items.length || items.some((item) => !item.productId || !Number.isInteger(item.quantity) || item.quantity < 1)) {
      api.showError(error, new Error("Choose a product and valid quantity for each sale item."));
      return;
    }
    const requestedByProduct = new Map();
    for (const item of items) {
      requestedByProduct.set(item.productId, (requestedByProduct.get(item.productId) || 0) + item.quantity);
    }
    const unavailable = [...requestedByProduct.entries()].find(([productId, quantity]) => {
      const product = products.find((candidate) => candidate.id === productId);
      return !product || quantity > product.quantity;
    });
    if (unavailable) {
      const product = products.find((candidate) => candidate.id === unavailable[0]);
      api.showError(error, new Error(`${product?.name || "Selected product"} has ${product?.quantity ?? 0} in stock; ${unavailable[1]} requested.`));
      return;
    }
    if (!window.confirm(`Record this sale for ${api.money(document.getElementById("sale-total").textContent)}? Stock will be deducted immediately.`)) return;
    submitButton.disabled = true;
    try {
      const sale = await api.post("/api/sales", { items });
      success.textContent = `Sale #${sale.id} recorded. Total: ${api.money(sale.total)}.`;
      success.hidden = false;
      lines.replaceChildren();
      addLine();
      await Promise.all([loadSales(), loadProducts()]);
    } catch (requestError) {
      api.showError(error, requestError);
    } finally {
      submitButton.disabled = false;
    }
  });

  async function loadProducts() {
    try {
      products = await api.get("/api/products");
      lines.replaceChildren();
      const sellableProducts = products.some((product) => product.quantity > 0);
      if (sellableProducts) {
        addLine();
      } else {
        lines.innerHTML = products.length
          ? '<p class="empty">There is no available stock to sell. Receive stock from Inventory first.</p>'
          : '<p class="empty">Add a product before recording a sale.</p>';
      }
      document.getElementById("add-sale-line").disabled = !sellableProducts;
      submitButton.disabled = !sellableProducts;
    } catch (requestError) {
      api.showError(error, requestError);
      lines.innerHTML = '<p class="empty">Products could not be loaded. Try refreshing this page.</p>';
      document.getElementById("add-sale-line").disabled = true;
      submitButton.disabled = true;
    }
  }

  document.getElementById("sales-rows").addEventListener("click", async (event) => {
    const button = event.target.closest("[data-details]");
    if (!button) return;
    try {
      const sale = await api.get(`/api/sales/${button.dataset.details}`);
      document.getElementById("sale-details").innerHTML = `<p class="eyebrow">TRANSACTION DETAILS</p>
        <h2>Sale #${sale.id}</h2><p class="muted">${api.esc(api.date(sale.createdAt))}</p>
        <div class="table-wrap"><table><thead><tr><th>Product</th><th class="numeric">Quantity</th><th class="numeric">Price</th><th class="numeric">Subtotal</th></tr></thead>
        <tbody>${sale.items.map((item) => `<tr><td>${api.esc(item.productName)}</td><td class="numeric">${item.quantity}</td><td class="numeric">${api.money(item.unitPrice)}</td><td class="numeric">${api.money(item.lineTotal)}</td></tr>`).join("")}
        <tr><td colspan="3"><strong>Total</strong></td><td class="numeric"><strong>${api.money(sale.total)}</strong></td></tr></tbody></table></div>`;
      document.getElementById("sale-dialog").showModal();
    } catch (requestError) {
      api.showError(error, requestError);
    }
  });

  loadProducts();
  loadSales();
});
