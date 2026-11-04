document.addEventListener("DOMContentLoaded", async () => {
  const api = window.StockAPI;
  const error = document.getElementById("alerts-error");
  const rows = document.getElementById("alert-rows");
  try {
    const products = await api.get("/api/alerts/low-stock");
    document.getElementById("alert-total").textContent = products.length;
    rows.innerHTML = products.length ? products.map((product) => `<tr>
      <td><strong>${api.esc(product.name)}</strong></td><td>${api.esc(product.sku)}</td>
      <td class="numeric"><span class="badge warning">${product.quantity}</span></td>
      <td class="numeric">${product.lowStockThreshold}</td>
      <td>${product.quantity === 0 ? '<span class="badge critical">Out of stock</span>' : '<span class="badge warning">Low stock</span>'}</td>
      <td>${api.esc(product.supplierName || "—")}</td>
      <td class="numeric"><a class="button secondary small" href="/inventory.html">Restock</a></td>
    </tr>`).join("") : '<tr><td colspan="7" class="empty">No low-stock products. Your inventory is in good shape.</td></tr>';
  } catch (requestError) {
    api.showError(error, requestError);
    rows.innerHTML = '<tr><td colspan="7" class="empty">Alerts could not be loaded.</td></tr>';
  }
});