package com.loyalstring.rfid.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.loyalstring.rfid.R

enum class Screens(val route: String) {
    LoginScreen("login"),
    HomeScreen("home"),
    ProductManagementScreen("products"),
    AddProductScreen("add product"),
    BulkProductScreen("bulk products"),
    ImportExcelScreen("import excel"),
    ProductListScreen("product list"),
    ScanToDesktopScreen("scan_web"),
    ScanDisplayScreen("scan_display"),
    SettingsScreen("settings"),
    InventoryMenuScreen("inventory"),
    EditProductScreen("edit_screen"),
    OrderScreen("order"),
    InvoiceScreen("invoiceScreen"),
    StockTransferScreen("stock_transfer"),
    OrderListScreen("order_list"),
    DailyRatesEditorScreen("daily_rates_editor"),
    LocationListScreen("location_list"),
    StockInScreen("stock_in"),
    StockOutScreen("stock_out"),
    StockTransferDetailScreen("stock_transfer_detail"),
    DeliveryChalan("delivery_chalan"),
    SearchMenuScreen("search_screen"),
    SampleOutScreen("sample_out"),
    SearchScreen("search_screen/{mode}"),
    DeliveryChallanListScreen("delivery_challan_list_screen"),
    SampleOutListScreen("sample_out_list_screen"),
    SampleInScreen("sample_in"),
    SampleInListScreen("sample_in_list"),
    QuotationScreen("quotation_screen"),
    QuotationListScreen("quotation_list"),
    StockVerificationReport("stockverification_report"),
    BatchDetailsScreen("batch_details_screen")
}

data class NavItems(
    val titleResId: Int,
    val unselectedIcon: ImageVector,
    val selectedIcon: Int,
    val route: String
)

val listOfNavItems = listOf(
    NavItems(
        titleResId = R.string.home,
        unselectedIcon = Icons.Outlined.Home,
        selectedIcon = R.drawable.home_svg,
        route = Screens.HomeScreen.route
    ),
    NavItems(
        titleResId = R.string.product,
        unselectedIcon = Icons.Outlined.MailOutline,
        selectedIcon = R.drawable.product_gr_svg,
        route = Screens.ProductManagementScreen.route
    ),
    NavItems(
        titleResId = R.string.inventory,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        selectedIcon = R.drawable.inventory_gr_svg,
        route = "inventory"
    ),
    NavItems(
        titleResId = R.string.order,
        unselectedIcon = Icons.Outlined.Settings,
        selectedIcon = R.drawable.order_gr_svg,
        route = Screens.OrderScreen.route
    ),
    NavItems(
        titleResId = R.string.search,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        selectedIcon = R.drawable.search_gr_svg,
        route = Screens.SearchScreen.route
    ),
    NavItems(
        titleResId = R.string.stock_transfer,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        selectedIcon = R.drawable.stock_tr_gr_svg,
        route = "stock_transfer"
    ),
    NavItems(
        titleResId = R.string.report,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        selectedIcon = R.drawable.report_gr_svg,
        route = Screens.StockVerificationReport.route
    ),
    NavItems(
        titleResId = R.string.quotations,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        selectedIcon = R.drawable.quotation_gr_svg,
        route = Screens.QuotationScreen.route
    ),
    NavItems(
        titleResId = R.string.delivery_challan,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        selectedIcon = R.drawable.delivery_challan_icon,
        route = Screens.DeliveryChalan.route
    ),
    NavItems(
        titleResId = R.string.invoice,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        selectedIcon = R.drawable.invoice_gr_svg,
        route = ""
    ),
    NavItems(
        titleResId = R.string.sample_in,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        selectedIcon = R.drawable.sample_in_gr_svg,
        route = "sample_in"
    ),
    NavItems(
        titleResId = R.string.sample_out,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        selectedIcon = R.drawable.sample_out_gr_svg,
        route = "sample_out"
    ),
    NavItems(
        titleResId = R.string.settings,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
        selectedIcon = R.drawable.setting_gr_svg,
        route = "settings"
    ),
    NavItems(
        titleResId = R.string.logout,
        unselectedIcon = Icons.AutoMirrored.Default.Logout,
        selectedIcon = R.drawable.logout,
        route = "login"
    )
)