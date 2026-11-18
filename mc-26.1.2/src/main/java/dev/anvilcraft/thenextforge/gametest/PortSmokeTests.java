package dev.anvilcraft.thenextforge.gametest;

import dev.anvilcraft.thenextforge.TheNextForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** 26.1.2 移植的冒烟测试：先证明测试框架本身在这套环境里能跑。 */
public final class PortSmokeTests {
    private PortSmokeTests() {
    }

    public static void registerAll(RegisterGameTestsEvent registerEvent) {
        ForgingGameTests.registerAll(registerEvent);
        TestRegistry.register(registerEvent, "api_targets_bases", ApiContractTests::mappedTargetsBasesAndRealStrike);
        TestRegistry.register(registerEvent, "api_upgrade_rollback", ApiContractTests::customUpgradeRollbackAndFailure);
        TestRegistry.register(registerEvent, "api_embedding_snapshot", ApiContractTests::embeddingSnapshotAndCallbackState);
        TestRegistry.register(registerEvent, "api_validation", ApiContractTests::validationAndEmbedFailure);
        TestRegistry.register(registerEvent, "scaling_display_single_factor", ScalingContractTests::displaySingleFactor);
        TestRegistry.register(registerEvent, "manual_feedback_transport", StrikeContractTests::feedbackTransport);
        TestRegistry.register(registerEvent, "quality_affixes_anvil", QualityContractTests::affixesAndVanillaAnvil);
        TestRegistry.register(registerEvent, "quality_glint_rollback", QualityContractTests::qualityGlintRollback);
        TestRegistry.register(registerEvent, "ticker_contract", TickerContractTests::tickerProbe);
        TestRegistry.register(registerEvent, "strike_royal_frost", StrikeContractTests::royalAndFrost);
        TestRegistry.register(registerEvent, "strike_dual_roles", StrikeContractTests::emberAndDualRoles);
        TestRegistry.register(registerEvent, "strike_failure_ledger", StrikeContractTests::failureLedgerAndSeal);
        TestRegistry.register(registerEvent, "strike_dog_refunds", StrikeContractTests::dogRefundAcrossOutcomes);
        TestRegistry.register(registerEvent, "contract_automation_payment", EquipmentContractTests::automationAndPayment);
        TestRegistry.register(registerEvent, "contract_carried_attributes", EquipmentContractTests::carriedAttributesAndCleanup);
        TestRegistry.register(registerEvent, "contract_effect_stacks", EquipmentContractTests::effectStacksAndScope);
        TestRegistry.register(registerEvent, "contract_frame_attachment", EquipmentContractTests::frameAttachmentAndUniqueness);
        TestRegistry.register(registerEvent, "contract_durability_history", ReversalContractTests::durabilityHistory);
        TestRegistry.register(registerEvent, "contract_repeated_embed_removal", ReversalContractTests::repeatedEmbedRemoval);
        PortAdapterTests.registerAll(registerEvent);
        WiringAuditTests.registerAll(registerEvent);
        TestRegistry.register(registerEvent, "mod_loads_and_tests_run", helper -> {
            helper.assertTrue(TheNextForge.MOD_ID.equals("thenextforge"), "mod id 应保持为 thenextforge");
            helper.succeed();
        });
    }
}
