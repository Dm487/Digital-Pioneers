document.addEventListener("DOMContentLoaded", () => {
  const api = window.StockAPI;
  const error = document.getElementById("inventory-error");
  const success = document.getElementById("inventory-success");
  const form = document.getElementById("restock-form");

  async function loadInventory() {
    const table = document.getElementById("inventory-rows");
    try {
      const products = await api.get("/api/inventory");
      const select = document.getElementById("restock-product");
      const selected = select.value;
      select.innerHTML = '<option value="">Choose a product</option>' + products.map((product) =>
        `<option value="${product.id}">${api.esc(product.name)} (${api.esc(product.sku)}) — ${product.quantity} in stock</option>`).join("");
      if (selected) select.value = selected;
      table.innerHTML = products.length ? products.map((product) => `<tr>
        <td><strong>${api.esc(product.name)}</strong></td><td>${api.esc(product.sku)}</td>
        <td class="numeric">${product.quantity}</td><td class="numeric">${product.lowStockThreshold}</td>
        <td>${product.quantity === 0 ? '<span class="badge critical">Out of stock</span>' : product.quantity <= product.lowStockThreshold ? '<span class="badge warning">Low stock</span>' : '<span class="badge good">In stock</span>'}</td>
      </tr>`).join("") : '<tr><td colspan="5" class="empty">Add products to start tracking inventory.</td></tr>';
    } catch (requestError) {
      api.showError(error, requestError);
      table.innerHTML = '<tr><td colspan="5" class="empty">Inventory could not be loaded.</td></tr>';
    }
  }

  async function loadMovements() {
    const table = document.getElementById("movement-rows");
    try {
      const movements = await api.get("/api/stock/movements?limit=100");
      table.innerHTML = movements.length ? movements.map((movement) => `<tr>
        <td>${api.esc(movement.productName)} <span class="subtext">${api.esc(movement.sku)}</span></td>
        <td>${movement.movementType === "SALE" ? "Sale" : "Restock"}</td>
        <td class="numeric ${movement.quantityDelta < 0 ? "quantity-negative" : "quantity-positive"}">${movement.quantityDelta > 0 ? "+" : ""}${movement.quantityDelta}</td>
        <td>${api.esc(movement.note || "—")}</td><td>${api.esc(api.date(movement.createdAt))}</td>
      </tr>`).join("") : '<tr><td colspan="5" class="empty">No stock movements have been recorded yet.</td></tr>';
    } catch (requestError) {
      api.showError(error, requestError);
      table.innerHTML = '<tr><td colspan="5" class="empty">Stock movement history could not be loaded.</td></tr>';
    }
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    api.clearError(error);
    api.clearError(success);
    const data = new FormData(form);
    const button = form.querySelector('[type="submit"]');
    button.disabled = true;
    try {
      const product = await api.post("/api/stock/restock", {
        productId: Number(data.get("productId")),
        quantity: Number(data.get("quantity")),
        note: data.get("note") || null
      });
      success.textContent = `${product.name} restocked. New quantity: ${product.quantity}.`;
      success.hidden = false;
      form.elements.quantity.value = "";
      form.elements.note.value = "";
      await Promise.all([loadInventory(), loadMovements()]);
    } catch (requestError) {
      api.showError(error, requestError);
    } finally {
      button.disabled = false;
    }
  });

  loadInventory();
  loadMovements();
});