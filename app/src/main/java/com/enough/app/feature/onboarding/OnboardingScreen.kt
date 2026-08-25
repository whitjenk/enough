package com.enough.app.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enough.app.R
import com.enough.app.feature.reminder.ReminderScheduler
import com.enough.app.data.model.ActivityGoalType
import com.enough.app.data.model.DietaryRestriction
import com.enough.app.di.AppViewModelProvider
import com.enough.app.domain.reminder.ReminderTimeOption
import com.enough.app.domain.risk.AgeBand
import com.enough.app.domain.risk.RiskScore
import com.enough.app.domain.risk.RiskScorer
import com.enough.app.domain.risk.Sex
import com.enough.app.health.HealthConnectAvailability
import com.enough.app.ui.components.ChoiceList
import com.enough.app.ui.components.ChoiceOption
import com.enough.app.ui.components.LabeledSlider
import com.enough.app.ui.components.MultiChoiceList
import com.enough.app.ui.components.SectionCard
import com.enough.app.ui.theme.EnoughTheme
import kotlin.math.roundToInt

/**
 * Onboarding entry point: collects [OnboardingUiState], wires the Health Connect
 * permission launcher, and navigates away once onboarding is persisted.
 */
@Composable
fun OnboardingRoute(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionContract = remember { viewModel.permissionsRequestContract() }
    val permissionLauncher = rememberLauncherForActivityResult(permissionContract) {
        viewModel.onPermissionsResult()
    }

    // The daily reminder needs POST_NOTIFICATIONS on Android 13+. Asked for only
    // when the person has just said yes to reminders, so the system prompt has
    // an obvious reason to be there; a denial is taken as final (SPEC §7.8).
    fun enableReminder(granted: Boolean) {
        viewModel.acceptReminder(granted)
        if (granted) {
            ReminderScheduler.schedule(context, uiState.reminderTime.minuteOfDay)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> enableReminder(granted) }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onComplete()
    }

    OnboardingScreen(
        uiState = uiState,
        onStartDefault = viewModel::startDefaultPath,
        onStartRiskTest = viewModel::startRiskTestPath,
        onRiskFormChange = viewModel::onRiskFormChange,
        onSubmitRiskTest = viewModel::submitRiskTest,
        onRiskResultContinue = viewModel::goToGoals,
        onGoalsFormChange = viewModel::onGoalsFormChange,
        onGoalsContinue = viewModel::goToExtras,
        onExtrasFormChange = viewModel::onExtrasFormChange,
        onExtrasContinue = viewModel::goToReminder,
        onReminderTimeChange = viewModel::onReminderTimeChange,
        onAcceptReminder = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                enableReminder(granted = true)
            }
        },
        onDeclineReminder = viewModel::declineReminder,
        onConnectHealth = { permissionLauncher.launch(viewModel.healthConnectPermissions) },
        onFinish = viewModel::finishOnboarding,
        onBack = viewModel::back,
    )
}

/** Stateless onboarding UI, dispatched by step. */
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onStartDefault: () -> Unit,
    onStartRiskTest: () -> Unit,
    onRiskFormChange: (RiskTestForm) -> Unit,
    onSubmitRiskTest: () -> Unit,
    onRiskResultContinue: () -> Unit,
    onGoalsFormChange: (GoalsForm) -> Unit,
    onGoalsContinue: () -> Unit,
    onExtrasFormChange: (ExtrasForm) -> Unit,
    onExtrasContinue: () -> Unit,
    onReminderTimeChange: (ReminderTimeOption) -> Unit,
    onAcceptReminder: () -> Unit,
    onDeclineReminder: () -> Unit,
    onConnectHealth: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
) {
    when (uiState.step) {
        OnboardingStep.WELCOME -> WelcomeStep(
            onStartDefault = onStartDefault,
            onStartRiskTest = onStartRiskTest,
        )
        OnboardingStep.RISK_TEST -> RiskTestStep(
            form = uiState.riskForm,
            onFormChange = onRiskFormChange,
            onSubmit = onSubmitRiskTest,
            onBack = onBack,
        )
        OnboardingStep.RISK_RESULT -> RiskResultStep(
            score = uiState.riskScore,
            onContinue = onRiskResultContinue,
            onBack = onBack,
        )
        OnboardingStep.GOALS -> GoalsStep(
            form = uiState.goalsForm,
            onFormChange = onGoalsFormChange,
            onContinue = onGoalsContinue,
            onBack = onBack,
        )
        OnboardingStep.EXTRAS -> ExtrasStep(
            form = uiState.extrasForm,
            onFormChange = onExtrasFormChange,
            onContinue = onExtrasContinue,
            onBack = onBack,
        )
        OnboardingStep.REMINDER -> ReminderStep(
            selected = uiState.reminderTime,
            onTimeChange = onReminderTimeChange,
            onAccept = onAcceptReminder,
            onDecline = onDeclineReminder,
            onBack = onBack,
        )
        OnboardingStep.HEALTH_CONNECT -> HealthConnectStep(
            state = uiState.healthConnect,
            isSaving = uiState.isSaving,
            onConnect = onConnectHealth,
            onFinish = onFinish,
            onBack = onBack,
        )
    }
}

// --- Shared scaffold ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingScaffold(
    title: String,
    onBack: (() -> Unit)?,
    primaryLabel: String,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                    }
                },
            )
        },
        bottomBar = {
            Surface {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Button(
                        onClick = onPrimary,
                        enabled = primaryEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(primaryLabel) }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}

// --- Steps ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeStep(onStartDefault: () -> Unit, onStartRiskTest: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.onboarding_welcome_title)) }) },
        bottomBar = {
            Surface {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Default path is the emphasized primary; the risk test is an
                    // equally-visible-but-secondary option, never the default.
                    Button(onClick = onStartDefault, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_entry_default))
                    }
                    OutlinedButton(onClick = onStartRiskTest, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_entry_risk))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome_body),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.onboarding_entry_default_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.onboarding_entry_risk_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            Text(
                text = stringResource(R.string.onboarding_welcome_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RiskTestStep(
    form: RiskTestForm,
    onFormChange: (RiskTestForm) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    OnboardingScaffold(
        title = stringResource(R.string.onboarding_risk_title),
        onBack = onBack,
        primaryLabel = stringResource(R.string.onboarding_see_result),
        primaryEnabled = form.isComplete,
        onPrimary = onSubmit,
    ) {
        Text(
            text = stringResource(R.string.onboarding_risk_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionCard(stringResource(R.string.risk_q_age)) {
            ChoiceList(
                options = listOf(
                    ChoiceOption(AgeBand.UNDER_40, stringResource(R.string.age_under_40)),
                    ChoiceOption(AgeBand.AGE_40_49, stringResource(R.string.age_40_49)),
                    ChoiceOption(AgeBand.AGE_50_59, stringResource(R.string.age_50_59)),
                    ChoiceOption(AgeBand.AGE_60_PLUS, stringResource(R.string.age_60_plus)),
                ),
                selected = form.ageBand,
                onSelect = { onFormChange(form.copy(ageBand = it)) },
            )
        }

        SectionCard(stringResource(R.string.risk_q_sex)) {
            Text(
                text = stringResource(R.string.risk_q_sex_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ChoiceList(
                options = listOf(
                    ChoiceOption(Sex.FEMALE, stringResource(R.string.sex_female)),
                    ChoiceOption(Sex.MALE, stringResource(R.string.sex_male)),
                ),
                selected = form.sex,
                onSelect = { onFormChange(form.copy(sex = it)) },
            )
        }

        if (form.sex == Sex.FEMALE) {
            SectionCard(stringResource(R.string.risk_q_gestational)) {
                YesNoChoice(
                    selected = form.hadGestationalDiabetes,
                    onSelect = { onFormChange(form.copy(hadGestationalDiabetes = it)) },
                )
            }
        }

        SectionCard(stringResource(R.string.risk_q_family)) {
            YesNoChoice(
                selected = form.familyHistoryDiabetes,
                onSelect = { onFormChange(form.copy(familyHistoryDiabetes = it)) },
            )
        }

        SectionCard(stringResource(R.string.risk_q_bp)) {
            YesNoChoice(
                selected = form.highBloodPressure,
                onSelect = { onFormChange(form.copy(highBloodPressure = it)) },
            )
        }

        SectionCard(stringResource(R.string.risk_q_active)) {
            YesNoChoice(
                selected = form.physicallyActive,
                onSelect = { onFormChange(form.copy(physicallyActive = it)) },
            )
        }

        SectionCard(stringResource(R.string.risk_q_height)) {
            val totalInches = form.heightFeet * 12 + form.heightInches
            LabeledSlider(
                valueLabel = stringResource(
                    R.string.height_value,
                    form.heightFeet,
                    form.heightInches,
                ),
                value = totalInches,
                valueRange = 48..84,
                step = 1,
                onValueChange = { inches ->
                    onFormChange(form.copy(heightFeet = inches / 12, heightInches = inches % 12))
                },
            )
        }

        SectionCard(stringResource(R.string.risk_q_weight)) {
            OutlinedTextField(
                value = form.weightLbText,
                onValueChange = { onFormChange(form.copy(weightLbText = it.filter { c -> c.isDigit() || c == '.' })) },
                label = { Text(stringResource(R.string.risk_weight_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.risk_asian_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun YesNoChoice(selected: Boolean, onSelect: (Boolean) -> Unit) {
    ChoiceList(
        options = listOf(
            ChoiceOption(true, stringResource(R.string.option_yes)),
            ChoiceOption(false, stringResource(R.string.option_no)),
        ),
        selected = selected,
        onSelect = onSelect,
    )
}

@Composable
private fun RiskResultStep(
    score: RiskScore?,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    OnboardingScaffold(
        title = stringResource(R.string.risk_result_title),
        onBack = onBack,
        primaryLabel = stringResource(R.string.onboarding_set_goals),
        primaryEnabled = score != null,
        onPrimary = onContinue,
    ) {
        if (score != null) {
            Text(
                text = stringResource(R.string.risk_result_score, score.total, RiskScorer.MAX_SCORE),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (score.isHighRisk) {
                    stringResource(R.string.risk_result_higher)
                } else {
                    stringResource(R.string.risk_result_lower)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            HorizontalDivider()
            Text(
                text = stringResource(R.string.risk_result_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GoalsStep(
    form: GoalsForm,
    onFormChange: (GoalsForm) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    OnboardingScaffold(
        title = stringResource(R.string.goals_title),
        onBack = onBack,
        primaryLabel = stringResource(R.string.action_continue),
        primaryEnabled = form.isComplete,
        onPrimary = onContinue,
    ) {
        SectionCard(stringResource(R.string.goals_weight_header)) {
            Text(
                text = stringResource(R.string.goals_weight_choice_desc),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            // Equally-weighted choice; focusing on fiber/activity is the default,
            // a weight goal is never turned on for the person (SPEC §3).
            ChoiceList(
                options = listOf(
                    ChoiceOption(false, stringResource(R.string.goals_weight_skip)),
                    ChoiceOption(true, stringResource(R.string.goals_weight_include)),
                ),
                selected = form.includeWeightGoal,
                onSelect = { onFormChange(form.copy(includeWeightGoal = it)) },
            )
            if (form.includeWeightGoal) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = form.weightLbText,
                    onValueChange = {
                        onFormChange(form.copy(weightLbText = it.filter { c -> c.isDigit() || c == '.' }))
                    },
                    label = { Text(stringResource(R.string.goals_weight_input_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                LabeledSlider(
                    valueLabel = stringResource(R.string.goals_weight_percent_label, form.weightLossPercent),
                    value = form.weightLossPercent,
                    valueRange = 5..7,
                    step = 1,
                    onValueChange = { onFormChange(form.copy(weightLossPercent = it)) },
                )
                val lb = form.weightLb
                if (lb != null) {
                    val targetLb = (lb * (1.0 - form.weightLossPercent / 100.0)).roundToInt()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.goals_weight_target,
                            stringResource(R.string.weight_pounds, lb.roundToInt()),
                            stringResource(R.string.weight_pounds, targetLb),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        SectionCard(stringResource(R.string.goals_activity_header)) {
            Text(
                text = stringResource(R.string.goals_activity_desc),
                style = MaterialTheme.typography.bodyMedium,
            )
            ChoiceList(
                options = listOf(
                    ChoiceOption(ActivityGoalType.MINUTES, stringResource(R.string.activity_type_minutes)),
                    ChoiceOption(ActivityGoalType.STEPS, stringResource(R.string.activity_type_steps)),
                    ChoiceOption(ActivityGoalType.CUSTOM, stringResource(R.string.activity_type_custom)),
                ),
                selected = form.activityGoalType,
                onSelect = { onFormChange(form.copy(activityGoalType = it)) },
            )
            Spacer(Modifier.height(8.dp))
            when (form.activityGoalType) {
                ActivityGoalType.MINUTES -> LabeledSlider(
                    valueLabel = stringResource(R.string.goals_activity_minutes_label, form.activityMinutes),
                    value = form.activityMinutes,
                    valueRange = 30..300,
                    step = 15,
                    onValueChange = { onFormChange(form.copy(activityMinutes = it)) },
                )
                ActivityGoalType.STEPS -> LabeledSlider(
                    valueLabel = stringResource(R.string.goals_activity_steps_label, form.activitySteps),
                    value = form.activitySteps,
                    valueRange = 2_000..15_000,
                    step = 500,
                    onValueChange = { onFormChange(form.copy(activitySteps = it)) },
                )
                ActivityGoalType.CUSTOM -> OutlinedTextField(
                    value = form.activityCustomLabel,
                    onValueChange = { onFormChange(form.copy(activityCustomLabel = it)) },
                    label = { Text(stringResource(R.string.goals_activity_custom_label)) },
                    placeholder = { Text(stringResource(R.string.goals_activity_custom_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SectionCard(stringResource(R.string.goals_fiber_header)) {
            OutlinedTextField(
                value = form.caloriesText,
                onValueChange = { onFormChange(form.copy(caloriesText = it.filter { c -> c.isDigit() })) },
                label = { Text(stringResource(R.string.goals_calories_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.goals_fiber_target, form.fiberTargetGrams),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.goals_fiber_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExtrasStep(
    form: ExtrasForm,
    onFormChange: (ExtrasForm) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    OnboardingScaffold(
        title = stringResource(R.string.extras_title),
        onBack = onBack,
        primaryLabel = stringResource(R.string.action_continue),
        primaryEnabled = true, // everything here is optional
        onPrimary = onContinue,
    ) {
        Text(
            text = stringResource(R.string.extras_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionCard(stringResource(R.string.extras_diet_header)) {
            Text(
                text = stringResource(R.string.extras_diet_desc),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            MultiChoiceList(
                options = listOf(
                    ChoiceOption(DietaryRestriction.VEGETARIAN, stringResource(R.string.diet_vegetarian)),
                    ChoiceOption(DietaryRestriction.VEGAN, stringResource(R.string.diet_vegan)),
                    ChoiceOption(DietaryRestriction.GLUTEN_FREE, stringResource(R.string.diet_gluten_free)),
                    ChoiceOption(DietaryRestriction.DAIRY_FREE, stringResource(R.string.diet_dairy_free)),
                    ChoiceOption(DietaryRestriction.NUT_ALLERGY, stringResource(R.string.diet_nut_allergy)),
                ),
                selected = form.dietaryRestrictions,
                onToggle = { onFormChange(form.toggleRestriction(it)) },
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = form.dietaryOther,
                onValueChange = { onFormChange(form.copy(dietaryOther = it)) },
                label = { Text(stringResource(R.string.extras_diet_other_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(stringResource(R.string.extras_why_header)) {
            Text(
                text = stringResource(R.string.extras_why_desc),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = form.personalWhy,
                onValueChange = { onFormChange(form.copy(personalWhy = it)) },
                label = { Text(stringResource(R.string.extras_why_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(stringResource(R.string.extras_glp1_header)) {
            Text(
                text = stringResource(R.string.extras_glp1_question),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            ChoiceList(
                options = listOf(
                    ChoiceOption(true, stringResource(R.string.option_yes)),
                    ChoiceOption(false, stringResource(R.string.option_no)),
                ),
                selected = form.takesGLP1,
                onSelect = { onFormChange(form.copy(takesGLP1 = it)) },
            )
        }
    }
}

/**
 * The one-time daily-reminder offer (SPEC §7.8).
 *
 * Both answers are real buttons of comparable weight, and the decline carries an
 * explicit promise that it won't be asked again — which the app keeps, via
 * `reminderOfferShown`. The body says plainly that the reminder backs off on its
 * own, because that is the unusual thing about it and the reason someone might
 * reasonably say yes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderStep(
    selected: ReminderTimeOption,
    onTimeChange: (ReminderTimeOption) -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.onboarding_reminder_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                },
            )
        },
        bottomBar = {
            Surface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_reminder_accept))
                    }
                    OutlinedButton(onClick = onDecline, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.onboarding_reminder_decline))
                    }
                    Text(
                        text = stringResource(R.string.onboarding_reminder_decline_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_reminder_body),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.onboarding_reminder_time_label),
                style = MaterialTheme.typography.titleMedium,
            )
            ChoiceList(
                options = ReminderTimeOption.entries.map { option ->
                    ChoiceOption(option, stringResource(option.labelRes()))
                },
                selected = selected,
                onSelect = onTimeChange,
            )
        }
    }
}

private fun ReminderTimeOption.labelRes(): Int = when (this) {
    ReminderTimeOption.MORNING -> R.string.reminder_time_morning
    ReminderTimeOption.MIDDAY -> R.string.reminder_time_midday
    ReminderTimeOption.AFTERNOON -> R.string.reminder_time_afternoon
    ReminderTimeOption.EVENING -> R.string.reminder_time_evening
}

@Composable
private fun HealthConnectStep(
    state: HealthConnectUiState,
    isSaving: Boolean,
    onConnect: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
) {
    OnboardingScaffold(
        title = stringResource(R.string.hc_title),
        onBack = onBack,
        primaryLabel = stringResource(R.string.onboarding_finish),
        primaryEnabled = !isSaving,
        onPrimary = onFinish,
    ) {
        Text(
            text = stringResource(R.string.hc_desc),
            style = MaterialTheme.typography.bodyLarge,
        )
        when (state.availability) {
            HealthConnectAvailability.AVAILABLE -> {
                if (state.permissionsGranted) {
                    Text(
                        text = stringResource(R.string.hc_connected),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    OutlinedButton(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.hc_connect))
                    }
                }
            }
            HealthConnectAvailability.UPDATE_REQUIRED -> Text(
                text = stringResource(R.string.hc_update_required),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HealthConnectAvailability.NOT_SUPPORTED -> Text(
                text = stringResource(R.string.hc_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// --- Previews ---

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() {
    EnoughTheme(dynamicColor = false) {
        OnboardingScreen(
            uiState = OnboardingUiState(step = OnboardingStep.WELCOME),
            onStartDefault = {}, onStartRiskTest = {}, onRiskFormChange = {}, onSubmitRiskTest = {},
            onRiskResultContinue = {}, onGoalsFormChange = {}, onGoalsContinue = {},
            onExtrasFormChange = {}, onExtrasContinue = {},
            onReminderTimeChange = {}, onAcceptReminder = {}, onDeclineReminder = {},
            onConnectHealth = {}, onFinish = {}, onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GoalsPreview() {
    EnoughTheme(dynamicColor = false) {
        OnboardingScreen(
            uiState = OnboardingUiState(
                step = OnboardingStep.GOALS,
                goalsForm = GoalsForm(includeWeightGoal = true, weightLbText = "180"),
            ),
            onStartDefault = {}, onStartRiskTest = {}, onRiskFormChange = {}, onSubmitRiskTest = {},
            onRiskResultContinue = {}, onGoalsFormChange = {}, onGoalsContinue = {},
            onExtrasFormChange = {}, onExtrasContinue = {},
            onReminderTimeChange = {}, onAcceptReminder = {}, onDeclineReminder = {},
            onConnectHealth = {}, onFinish = {}, onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderPreview() {
    EnoughTheme(dynamicColor = false) {
        OnboardingScreen(
            uiState = OnboardingUiState(step = OnboardingStep.REMINDER),
            onStartDefault = {}, onStartRiskTest = {}, onRiskFormChange = {}, onSubmitRiskTest = {},
            onRiskResultContinue = {}, onGoalsFormChange = {}, onGoalsContinue = {},
            onExtrasFormChange = {}, onExtrasContinue = {},
            onReminderTimeChange = {}, onAcceptReminder = {}, onDeclineReminder = {},
            onConnectHealth = {}, onFinish = {}, onBack = {},
        )
    }
}
