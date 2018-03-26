package com.constanttherapy.enums;

public enum UserEventType
{
    AccountCreated(1),
    EulaAccepted(2),
    ScheduleChanged(3),
    SessionCompleted(4),
    EmailSent(5),
    AccountMarkedInactive(6),
    AccountMarkedActive(7),
    ClinicianAddedToPatient(8),
    ClinicianRemovedFromPatient(9),
    PasswordChanged(10),

    UserLogin(11),
    AutomaticUserLogin(12, true),
    UserLogout(13, true),
    UserSharedResults(14, true),
    PatientDormancyAlerts(15),

    ClinicianAddedToGroup(16),
    ClinicianRemovedFromGroup(17),

    PatientDischarged(18),
    PatientDiscontinued(19),
    ClinicianSharedSoapReport(20, true),
    SubscriptionCreated(21),
    SubscriptionCanceled(22),
    TaskCountMilestone(23),
    UserForgotPassword(24),
    ExpiredAccountUseAttempted(25),

    UserVisitedSubscribePage(26),

	// REVIEW: this user event is not used
    /*UserSubscribed(27),*/

	PhoneConsultRequested(28),
    TrialExtended(29),
    MandrillWebhookEvent(30),
    DoNotContactClicked(31),
	ClientInfoUpdated(32),
    UserSubscriptionSubStatusChanged(33),
    UserInfoUpdated(34),
    UserGrandfathered(35),
    UserTaskStatsGenerated(36),
    AutomaticTaskProgression(38),
    SurveyResult(39),
    SurveyHit(40),
    RenewalChargeFailed(41),
    HelpUsed(42, true),
    ReportViewed(43, true),
    PatientSelected(44, true),

    FirebaseCloudMessageSent(45),
    FirebaseCloudMessageClick(46),
    FirebaseCloudMessageFailed(47),
    IosIapWebhook(48),
    SkipTaskItem(49, true),
    SkipTaskType(50, true);
    /**
     * Must match user event type unique id in database
     */
    private final int value;

    /**
     * Whether these user events can be logged directly from the client through an endpoint
     */
    private final boolean canClientNotify;

    UserEventType(int val)
    {
        this.value = val;
        this.canClientNotify = false;
    }

    UserEventType(int val, boolean clientNotify)
    {
        this.value = val;
        this.canClientNotify = clientNotify;
    }

    public int getValue()
    {
        return this.value;
    }

    public boolean getCanClientNotify() { return this.canClientNotify; }

}
