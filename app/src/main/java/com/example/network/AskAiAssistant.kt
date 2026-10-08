package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

object AskAiAssistant {

    suspend fun getAiAnswer(question: String): String = withContext(Dispatchers.Default) {
        val q = question.lowercase().trim()

        when {
            q.contains("add") && (q.contains("customer") || q.contains("record") || q.contains("inquiry")) -> {
                "📌 **To Add a New Customer / Inquiry:**\n1. From the bottom navigation bar or top dashboard, tap '+ New Customer'.\n2. Enter Customer ID (auto-generated or custom), Name, Mobile & WhatsApp numbers.\n3. Fill in Business details, Website Requirements, and Financials (Estimated, Confirm Amount).\n4. Pick a scheduled Follow-up date.\n5. Tap 'Save Customer Record'. The system will record the transaction and create the customer timeline."
            }
            q.contains("payment") && (q.contains("received") || q.contains("mark") || q.contains("record")) -> {
                "💰 **To Mark Payment as Received:**\n1. Open the Customer's profile from the list or dashboard.\n2. Tap the 'Record Payment' button in the Payment section.\n3. Enter the received amount (e.g., ₹25,000), payment method (Bank Wire, Cash, Online), and notes.\n4. Save it! The system automatically deducts pending amount, updates Payment Status to 'Payment Received' if cleared, and updates Dashboard total revenue."
            }
            q.contains("overdue") -> {
                "⏰ **What does Overdue Mean?**\nAn inquiry becomes **OVERDUE** when its scheduled follow-up date and time has passed without being completed or marked as Success/Cancelled.\n\nOverdue customers appear with a red badge on the Dashboard and Customer List. You can quickly trigger a WhatsApp reminder or dial them directly."
            }
            q.contains("whatsapp") -> {
                "📲 **How WhatsApp Integration Works:**\n- In any customer profile, tap 'WhatsApp Reminder'.\n- The app fills in a professional template with the customer's name, project details, and pending balance.\n- You can send it directly via WhatsApp app or configure a custom WhatsApp Cloud/Webhook API in Settings for automated overdue dispatch."
            }
            q.contains("status") -> {
                "🔄 **Customer & Project Status System:**\n- **New Inquiry**: Initial contact.\n- **Follow Up**: Needs subsequent discussion.\n- **Interested / Confirmed**: Proposal approved.\n- **In Progress**: Website under design/development.\n- **Payment Pending / Received**: Financial status.\n- **Success**: Completed website delivered and payment fully settled!"
            }
            q.contains("call") || q.contains("dial") -> {
                "📞 **Calling a Customer:**\nTap the 'Call Customer' button on their profile or customer card. The native Android dialer will open securely with their phone number pre-loaded so you can connect immediately. The app logs the call event in the customer's activity history."
            }
            q.contains("search") || q.contains("filter") -> {
                "🔍 **Smart Search & Filters:**\nGo to the 'Customers' tab. Type any name, mobile number, business name, or city. Use quick chips above the list to filter by 'Overdue', 'Today', 'Payment Pending', 'In Progress', or 'Success'."
            }
            q.contains("backup") || q.contains("restore") || q.contains("export") -> {
                "💾 **Backup & Restore System:**\nNavigate to Settings -> 'Backup & Data Management'. Tap 'Create Backup' to generate a full JSON export of all 15,000+ customers, payments, and activity logs. You can restore this backup anytime without data loss."
            }
            q.contains("15000") || q.contains("15,000") || q.contains("capacity") -> {
                "⚡ **15,000+ Customer Capacity:**\nWeb Record by Waqar is powered by indexed SQLite Room Database with transactions and memory-efficient LazyColumn rendering. It easily scales to tens of thousands of records without slowing down or losing data."
            }
            else -> {
                "💡 **Web Record by Waqar Help Guide:**\nI am your dedicated AI assistant for this application. You can ask me:\n• 'How do I add a new customer?'\n• 'How do I mark payment as received?'\n• 'What does overdue mean?'\n• 'How does the WhatsApp reminder work?'\n• 'How to search 15,000+ customer records?'\n• 'How to create a data backup?'"
            }
        }
    }
}
