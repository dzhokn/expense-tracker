package com.example.expensetracker.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CarRental
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DownhillSkiing
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlife
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIcons {

    val curatedIcons: Map<String, ImageVector> = mapOf(
        // Food & Drink
        "restaurant" to Icons.Default.Restaurant,
        "dining" to Icons.Default.DinnerDining,
        "coffee" to Icons.Default.Coffee,
        "local_grocery_store" to Icons.Default.LocalGroceryStore,
        "local_bar" to Icons.Default.LocalBar,
        "bakery_dining" to Icons.Default.BakeryDining,

        // Transport & Vehicle
        "directions_car" to Icons.Default.DirectionsCar,
        "train" to Icons.Default.Train,
        "flight" to Icons.Default.Flight,
        "local_gas_station" to Icons.Default.LocalGasStation,
        "local_parking" to Icons.Default.LocalParking,
        "directions_bus" to Icons.Default.DirectionsBus,
        "local_taxi" to Icons.Default.LocalTaxi,
        "local_car_wash" to Icons.Default.LocalCarWash,
        "car_rental" to Icons.Default.CarRental,

        // Home & Utilities
        "home" to Icons.Default.Home,
        "apartment" to Icons.Default.Apartment,
        "bolt" to Icons.Default.Bolt,
        "water_drop" to Icons.Default.WaterDrop,
        "wifi" to Icons.Default.Wifi,
        "phone_android" to Icons.Default.PhoneAndroid,
        "build" to Icons.Default.Build,
        "power" to Icons.Default.Power,
        "local_fire_department" to Icons.Default.LocalFireDepartment,

        // Health & Fitness
        "local_hospital" to Icons.Default.LocalHospital,
        "medical_services" to Icons.Default.MedicalServices,
        "medication" to Icons.Default.Medication,
        "fitness_center" to Icons.Default.FitnessCenter,
        "shield" to Icons.Default.Shield,
        "spa" to Icons.Default.Spa,

        // Shopping & Personal
        "shopping_cart" to Icons.Default.ShoppingCart,
        "checkroom" to Icons.Default.Checkroom,
        "devices" to Icons.Default.Devices,
        "content_cut" to Icons.Default.ContentCut,
        "dry_cleaning" to Icons.Default.DryCleaning,
        "person" to Icons.Default.Person,
        "face" to Icons.Default.Face,
        "chair" to Icons.Default.Chair,
        "menu_book" to Icons.AutoMirrored.Filled.MenuBook,

        // Entertainment
        "movie" to Icons.Default.Movie,
        "theaters" to Icons.Default.Theaters,
        "sports_esports" to Icons.Default.SportsEsports,
        "live_tv" to Icons.Default.LiveTv,
        "music_note" to Icons.Default.MusicNote,
        "nightlife" to Icons.Default.Nightlife,
        "beach_access" to Icons.Default.BeachAccess,

        // Business & Government
        "business_center" to Icons.Default.BusinessCenter,
        "account_balance" to Icons.Default.AccountBalance,
        "gavel" to Icons.Default.Gavel,
        "receipt_long" to Icons.AutoMirrored.Filled.ReceiptLong,

        // Kids & Hobbies
        "child_care" to Icons.Default.ChildCare,
        "palette" to Icons.Default.Palette,
        "mic" to Icons.Default.Mic,
        "downhill_skiing" to Icons.Default.DownhillSkiing,
        "sports_soccer" to Icons.Default.SportsSoccer,

        // Gifts & Charity
        "volunteer_activism" to Icons.Default.VolunteerActivism,
        "favorite" to Icons.Default.Favorite,
        "card_giftcard" to Icons.Default.CardGiftcard,

        // Tech & Subscriptions
        "smart_toy" to Icons.Default.SmartToy,
        "cloud" to Icons.Default.Cloud,
        "watch" to Icons.Default.Watch,
        "subscriptions" to Icons.Default.Subscriptions,

        // Finance & Other
        "savings" to Icons.Default.Savings,
        "school" to Icons.Default.School,
        "more_horiz" to Icons.Default.MoreHoriz,
        "folder" to Icons.Default.Folder,
        "receipt" to Icons.Default.Receipt,
        "payments" to Icons.Default.Payments
    )

    /**
     * Maps every CSV category fullPath to its icon key.
     * Used by DatabaseSeeder and one-time icon migration.
     */
    val csvCategoryIcons: Map<String, String> = mapOf(
        // Root categories
        "Business" to "business_center",
        "Entertainment" to "movie",
        "Food" to "restaurant",
        "Gifts & Charity" to "volunteer_activism",
        "Government" to "account_balance",
        "Health" to "local_hospital",
        "Hobbies" to "palette",
        "Housing" to "home",
        "Kids" to "child_care",
        "Shopping" to "shopping_cart",
        "Transport" to "directions_bus",
        "Vanity" to "face",
        "Vehicle" to "directions_car",

        // Business
        "Business > Catwing" to "business_center",
        "Business > Other" to "more_horiz",
        "Business > SHKOLO" to "school",

        // Entertainment
        "Entertainment > Cinema & Culture" to "theaters",
        "Entertainment > Games" to "sports_esports",
        "Entertainment > Night life" to "nightlife",
        "Entertainment > Other" to "more_horiz",
        "Entertainment > Vacation" to "beach_access",

        // Food
        "Food > Eating out" to "dining",
        "Food > Groceries" to "local_grocery_store",

        // Gifts & Charity
        "Gifts & Charity > Charity" to "favorite",
        "Gifts & Charity > Gifts" to "card_giftcard",

        // Government
        "Government > Fines" to "gavel",
        "Government > Taxes" to "receipt_long",

        // Health
        "Health > Insurance" to "shield",
        "Health > Medical" to "medical_services",
        "Health > Pharmacy" to "medication",
        "Health > Supplements" to "medication",
        "Health > Wellness" to "spa",

        // Hobbies
        "Hobbies > ABLE" to "school",
        "Hobbies > Education" to "school",
        "Hobbies > Singing" to "mic",
        "Hobbies > Sport" to "fitness_center",
        "Hobbies > Sport > BJJ" to "fitness_center",
        "Hobbies > Sport > Dances" to "music_note",
        "Hobbies > Sport > Fitness" to "fitness_center",
        "Hobbies > Sport > Other" to "more_horiz",
        "Hobbies > Sport > Skiing" to "downhill_skiing",
        "Hobbies > Sport > Soccer" to "sports_soccer",

        // Housing
        "Housing > Rent/Mortgage" to "apartment",
        "Housing > Repairs & Maintenance" to "build",
        "Housing > Utilities" to "bolt",
        "Housing > Utilities > Building Fee" to "apartment",
        "Housing > Utilities > Electricity" to "power",
        "Housing > Utilities > Heating" to "local_fire_department",
        "Housing > Utilities > Internet & TV" to "wifi",
        "Housing > Utilities > Other" to "more_horiz",
        "Housing > Utilities > Phone" to "phone_android",
        "Housing > Utilities > Water Supply" to "water_drop",

        // Kids
        "Kids > Kaya" to "child_care",
        "Kids > Thea" to "child_care",

        // Shopping
        "Shopping > Books" to "menu_book",
        "Shopping > Electronics" to "devices",
        "Shopping > Home" to "chair",
        "Shopping > Subscriptions" to "subscriptions",
        "Shopping > Subscriptions > AI" to "smart_toy",
        "Shopping > Subscriptions > Google Drive" to "cloud",
        "Shopping > Subscriptions > Netflix" to "live_tv",
        "Shopping > Subscriptions > Other" to "more_horiz",
        "Shopping > Subscriptions > Oura" to "watch",
        "Shopping > Subscriptions > Pulsetto" to "watch",
        "Shopping > Subscriptions > Storytel" to "menu_book",
        "Shopping > Subscriptions > YouTube" to "live_tv",

        // Transport
        "Transport > Flights" to "flight",
        "Transport > Public Transport" to "train",
        "Transport > Taxi" to "local_taxi",

        // Vanity
        "Vanity > Barber" to "content_cut",
        "Vanity > Clothes" to "checkroom",
        "Vanity > Hair Removal" to "content_cut",

        // Vehicle
        "Vehicle > Car Wash" to "local_car_wash",
        "Vehicle > Car/Leasing" to "car_rental",
        "Vehicle > Fuel" to "local_gas_station",
        "Vehicle > Insurance" to "shield",
        "Vehicle > Parking" to "local_parking",
        "Vehicle > Repairs & Maintenance" to "build"
    )

    fun get(key: String): ImageVector {
        return curatedIcons[key] ?: Icons.Default.Category
    }

    fun allEntries(): List<Pair<String, ImageVector>> {
        return curatedIcons.entries.map { it.key to it.value }
    }
}
