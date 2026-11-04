document.addEventListener("DOMContentLoaded", async () => {
  const error = document.getElementById("dashboard-error");
  const api = window.StockAPI;
  const recentSales = document.getElementById("recent-sales");
  const lowStock = document.getElementById("low-stock");
  const movements = document.getElementById("recent-movements");
  try {
    const dashboard = await api.get("/api/dashboard");
    const { stats, recentSales: sales, lowStockProducts: alerts, recentMovements: stockMovements } = dashboard;
    document.getElementById("product-count").textContent = stats.totalProducts;
    document.getElementById("stock-count").textContent = stats.totalUnitsInStock;
    document.getElementById("alert-count").textContent = stats.lowStockProducts;
    document.getElementById("sales-total").textContent = api.money(stats.totalSales);
    document.getElementById("sales-count").textContent = stats.totalTransactions;
    recentSales.innerHTML = sales.length
      ? sales.map((sale) => `<tr id="sale-${sale.id}"><td><a class="table-link" href="/sales.html#sale-${sale.id}">Sale #${sale.id}</a></td><td>${api.esc(api.date(sale.createdAt))}</td><td>${sale.items.reduce((sum, item) => sum + item.quantity, 0)}</td><td class="numeric">${api.money(sale.total)}</td></tr>`).join("")
      : '<tr><td colspan="4" class="empty">No sales have been recorded yet.</td></tr>';
    lowStock.innerHTML = alerts.length
      ? alerts.map((product) => `<tr><td>${api.esc(product.name)}</td><td>${api.esc(product.sku)}</td><td class="numeric">${product.quantity}</td><td class="numeric">${product.lowStockThreshold}</td><td><span class="badge ${product.quantity === 0 ? "critical" : "warning"}">${product.quantity === 0 ? "Out of stock" : "Low stock"}</span></td></tr>`).join("")
      : '<tr><td colspan="5" class="empty">No low-stock products. Stock is above each reorder level.</td></tr>';
    movements.innerHTML = stockMovements.length
      ? stockMovements.map((movement) => `<tr><td>${api.esc(movement.productName)}</td><td>${movement.movementType === "SALE" ? "Sale" : "Restock"}</td><td class="numeric ${movement.quantityDelta < 0 ? "quantity-negative" : "quantity-positive"}">${movement.quantityDelta > 0 ? "+" : ""}${movement.quantityDelta}</td><td>${api.esc(movement.note || "—")}</td><td>${api.esc(api.date(movement.createdAt))}</td></tr>`).join("")
      : '<tr><td colspan="5" class="empty">No stock movements have been recorded yet.</td></tr>';
  } catch (requestError) {
    api.showError(error, requestError);
    recentSales.innerHTML = '<tr><td colspan="4" class="empty">Sales could not be loaded.</td></tr>';
    lowStock.innerHTML = '<tr><td colspan="5" class="empty">Stock alerts could not be loaded.</td></tr>';
    movements.innerHTML = '<tr><td colspan="5" class="empty">Stock movements could not be loaded.</td></tr>';
    for (const id of ["product-count", "stock-count", "alert-count", "sales-total"]) {
      document.getElementById(id).textContent = "Unavailable";
    }
  }
});