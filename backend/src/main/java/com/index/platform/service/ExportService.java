package com.index.platform.service;

import com.index.platform.dto.Dto;
import com.index.platform.security.AuthUser;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** CSV files that open directly in Excel. */
@Service
@RequiredArgsConstructor
public class ExportService {
    private final OrderService orders;
    private final SalesService sales;
    private final ReportService reports;

    private static String esc(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        if (o instanceof String && !s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static void row(StringBuilder sb, Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(esc(cells[i]));
        }
        sb.append("\r\n");
    }

    public String orders(AuthUser u) {
        StringBuilder sb = new StringBuilder();
        row(sb, "Order", "Crop", "Quantity", "Unit", "Shop", "Farmer", "Agreed price (RWF)", "Total (RWF)", "Delivery date",
                "Status", "Payment", "Payment method", "Delivery method", "Transport cost (RWF)", "Spoiled quantity", "Created");
        for (Dto.OrderResponse o : orders.list(u, null)) {
            row(sb, o.id(), o.cropName(), o.quantity(), o.unit(), o.shopName(), o.farmerName(), o.agreedPrice(), o.totalValue(),
                    o.deliveryDate(), o.status(), o.paymentStatus(), o.paymentMethod(), o.deliveryMethod(),
                    o.transportCost(), o.spoiledQuantity(), o.createdAt());
        }
        return sb.toString();
    }

    public String sales(AuthUser u) {
        StringBuilder sb = new StringBuilder();
        row(sb, "Date", "Crop", "Quantity", "Unit", "Price per unit (RWF)", "Total (RWF)");
        for (Dto.SalesResponse s : sales.list(u, LocalDate.now().minusYears(1), LocalDate.now())) {
            row(sb, s.soldOn(), s.cropName(), s.quantity(), s.unit(), s.unitPrice(), s.total());
        }
        return sb.toString();
    }

    public String weeklySummary(LocalDate weekStart) {
        Dto.WeeklySummary w = reports.weeklySummary(weekStart);
        StringBuilder sb = new StringBuilder();
        row(sb, "Week", w.weekStart() + " to " + w.weekEnd());
        row(sb, "Crop", "Requested", "Listed by farmers", "Sold by shops", "Balance (requested minus listed)");
        List<Dto.CropWeekSummary> crops = w.crops();
        for (Dto.CropWeekSummary c : crops) row(sb, c.cropName(), c.demandQty(), c.supplyQty(), c.soldQty(), c.gap());
        return sb.toString();
    }

    public String pilot() {
        Dto.PilotEconomics p = reports.pilotEconomics();
        StringBuilder sb = new StringBuilder();
        row(sb, "Measure", "Value");
        row(sb, "Total orders", p.totalOrders());
        row(sb, "Completed orders", p.completedOrders());
        row(sb, "Failed orders", p.failedOrders());
        row(sb, "Cancelled orders", p.cancelledOrders());
        row(sb, "Fulfilment rate (%)", p.fulfilmentRatePercent());
        row(sb, "Quantity delivered", p.completedQuantity());
        row(sb, "Value traded (RWF)", p.completedValue());
        row(sb, "Payments confirmed received (RWF)", p.paymentsReceivedValue());
        row(sb, "Completed but not yet paid (RWF)", p.unpaidCompletedValue());
        row(sb, "Transport spent (RWF)", p.totalTransportCost());
        row(sb, "Transport cost per completed order (RWF)", p.transportCostPerCompletedOrder());
        row(sb, "Spoiled quantity", p.spoiledQuantity());
        return sb.toString();
    }
}
