package ai.tabby.demoapp

import ai.tabby.android.data.Product
import ai.tabby.android.data.TabbyPayment
import ai.tabby.android.data.tabbyResult
import ai.tabby.demoapp.ui.CheckoutResultScreen
import ai.tabby.demoapp.ui.FailedScreen
import ai.tabby.demoapp.ui.ProductScreen
import ai.tabby.demoapp.ui.ProgressScreen
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState


/**
 * CheckoutActivity handles the checkout process using Jetpack Compose for UI rendering.
 *
 * This activity demonstrates the use of Jetpack Compose in conjunction with ComponentActivity
 * to create a modern, declarative UI for the checkout flow. It showcases several key Compose concepts:
 *
 * 1. State Management: Uses [collectAsState] to observe ViewModel state changes.
 * 2. Composable Functions: Utilizes custom Composable functions like [ProgressScreen], [ProductScreen], etc.
 * 3. Compose Integration: Employs [setContent] to set up the Compose UI hierarchy.
 * 4. ViewModel Integration: Demonstrates ViewModel usage with Compose using [viewModels] delegate.
 *
 * To use this activity:
 * 1. Ensure you have the necessary Compose dependencies in your project's build.gradle file.
 * 2. Pass a [TabbyPayment] object as an intent extra with the key [ARG_TABBY_PAYMENT].
 * 3. The activity will handle the checkout flow, including session creation, product selection, and result handling.
 *
 * Example usage:
 * ```
 * val intent = Intent(context, CheckoutActivity::class.java).apply {
 *     putExtra(CheckoutActivity.ARG_TABBY_PAYMENT, tabbyPayment)
 * }
 * startActivity(intent)
 * ```
 *
 * @see ComponentActivity
 * @see setContent
 * @see collectAsState
 * @see viewModels
 */
class CheckoutActivity : ComponentActivity() {

    companion object {
        const val ARG_TABBY_PAYMENT = "arg.tabbyPayment"
    }

    private val viewModel: CheckoutViewModel by viewModels()

    private val tabbyPayment: TabbyPayment by lazy {
        intent.getParcelableExtra<TabbyPayment>(ARG_TABBY_PAYMENT)
            ?: throw IllegalArgumentException("Argument $ARG_TABBY_PAYMENT is missing")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create tabby session
        viewModel.createSession(tabbyPayment)

        setContent {
            val state = viewModel.screenStateFlow.collectAsState()
            when (state.value.state) {
                ScreenState.State.INITIAL -> {}
                ScreenState.State.CREATING_SESSION -> ProgressScreen(
                    tabbyPayment = tabbyPayment
                )
                ScreenState.State.SESSION_CREATED -> ProductScreen(
                    viewModel = viewModel,
                    tabbyPayment = tabbyPayment,
                    onProductSelected = ::onProductSelected
                )
                ScreenState.State.SESSION_FAILED -> FailedScreen {
                    // Retry create session
                    viewModel.createSession(tabbyPayment = tabbyPayment)
                }
                ScreenState.State.CHECKOUT_RESULT -> CheckoutResultScreen(
                    viewModel = viewModel
                ) { finish() }
            }
        }
    }

    private fun onProductSelected(product: Product) {
        val i = viewModel.createCheckoutIntent(product)
        checkoutContract.launch(i)
    }

    private val checkoutContract =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    result.tabbyResult?.let { tabbyResult ->
                        viewModel.onCheckoutResult(tabbyResult)
                    } ?: Toast.makeText(this, "Tabby result is null", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Result is not OK", Toast.LENGTH_LONG).show()
                }
            }
        }

}