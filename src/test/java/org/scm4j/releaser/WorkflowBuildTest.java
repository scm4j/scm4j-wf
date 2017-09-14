package org.scm4j.releaser;

import org.junit.Test;
import org.scm4j.commons.Version;
import org.scm4j.commons.progress.IProgress;
import org.scm4j.commons.progress.ProgressConsole;
import org.scm4j.releaser.LogTag;
import org.scm4j.releaser.SCMReleaser;
import org.scm4j.releaser.actions.ActionNone;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranch;
import org.scm4j.releaser.branch.ReleaseBranchStatus;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.scmactions.ReleaseReason;
import org.scm4j.releaser.scmactions.SCMActionBuild;
import org.scm4j.releaser.scmactions.SCMActionFork;
import org.scm4j.vcs.api.VCSCommit;
import org.scm4j.vcs.api.VCSTag;
import org.scm4j.vcs.api.WalkDirection;

import static org.junit.Assert.*;

public class WorkflowBuildTest extends WorkflowTestBase {
	
	@Test
	public void testBuildAll() {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMWorkflow wf = new SCMWorkflow();

		// simulate BRANCHED dev branches statuses
		env.generateContent(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "test file", "test content", LogTag.SCM_VER);
		env.generateContent(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "test file", "test content", LogTag.SCM_VER);

		// fork unTill
		IAction action = wf.getProductionReleaseAction(UNTILL);
		action.execute(new NullProgress());
		checkUnTillForked();

		// build unTill
		action = wf.getProductionReleaseAction(UNTILL);
		action.execute(new NullProgress());
		checkUnTillBuilt();
	}

	@Test
	public void testBuildRootIfNestedIsBuiltAlready() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		
		// fork unTillDb 
		IAction action = releaser.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// build unTillDb
		action = releaser.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// fork UBL
		// simulate BRANCHED dev branch status
		env.generateContent(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "test file", "test content", LogTag.SCM_VER);
		action = releaser.getProductionReleaseAction(UBL);
		Expectations exp = new Expectations();
		exp.put(UBL, SCMActionFork.class);
		exp.put(UBL, "fromstatus", ReleaseBranchStatus.MISSING);
		exp.put(UBL, "tostatus", ReleaseBranchStatus.MDEPS_ACTUAL);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		checkUBLForked();
		
		// build UBL
		action = releaser.getProductionReleaseAction(UBL);
		exp = new Expectations();
		exp.put(UBL, SCMActionBuild.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_FEATURES);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		} 
		checkUBLBuilt();
	}

	@Test
	public void testBuildUBLAndUnTillDb() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		
		// fork unTillDb
		IAction action = releaser.getProductionReleaseAction(UNTILLDB);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, SCMActionFork.class);
		exp.put(UNTILLDB, "fromstatus", ReleaseBranchStatus.MISSING);
		exp.put(UNTILLDB, "tostatus", ReleaseBranchStatus.MDEPS_ACTUAL);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		// fork UBL
		// simulate BRANCHED dev branch status
		env.generateContent(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "test file", "test content", LogTag.SCM_VER);
		action = releaser.getProductionReleaseAction(UBL);
		exp = new Expectations();
		exp.put(UBL, SCMActionFork.class);
		exp.put(UBL, "fromstatus", ReleaseBranchStatus.MISSING);
		exp.put(UBL, "tostatus", ReleaseBranchStatus.MDEPS_ACTUAL);
		exp.put(UNTILLDB, ActionNone.class);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		assertTrue(TestBuilder.getBuilders().isEmpty());
		
		// build UBL and unTillDb
		action = releaser.getProductionReleaseAction(UBL);
		exp = new Expectations();
		exp.put(UBL, SCMActionBuild.class);
		exp.put(UBL, "reason", ReleaseReason.NEW_DEPENDENCIES);
		exp.put(UNTILLDB, SCMActionBuild.class);
		exp.put(UNTILLDB, "reason", ReleaseReason.NEW_FEATURES);
		checkChildActionsTypes(action, exp);
		
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		assertNotNull(TestBuilder.getBuilders());
		assertTrue(TestBuilder.getBuilders().size() == 2);
		assertNotNull(TestBuilder.getBuilders().get(UNTILLDB));
		assertNotNull(TestBuilder.getBuilders().get(UBL));
	}
	
	@Test
	public void testBuildSingleComponent() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		IAction action = releaser.getProductionReleaseAction(UNTILLDB);
		
		// fork unTillDb release
		action.execute(new NullProgress());
		
		action = releaser.getProductionReleaseAction(UNTILLDB);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, SCMActionBuild.class);
		exp.put(UNTILLDB, "reason", ReleaseReason.NEW_FEATURES);
		checkChildActionsTypes(action, exp);

		// build unTillDb
		assertTrue(TestBuilder.getBuilders().isEmpty());
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		
		assertNotNull(TestBuilder.getBuilders());
		assertTrue(TestBuilder.getBuilders().size() == 1);
		assertNotNull(TestBuilder.getBuilders().get(UNTILLDB));
		
		// check versions
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		Version verRelease = rbUnTillDb.getCurrentVersion();
		assertEquals(env.getUnTillDbVer().toNextPatch().toReleaseString(), verRelease.toString());
		
		// check tags
		List<VCSTag> tags = env.getUnTillDbVCS().getTags();
		assertTrue(tags.size() == 1);
		VCSTag tag = tags.get(0);
		assertEquals(env.getUnTillDbVer().toReleaseString(), tag.getTagName());
		List<VCSCommit> commits = env.getUnTillDbVCS().getCommitsRange(rbUnTillDb.getName(), null, WalkDirection.DESC, 2);
		assertEquals(commits.get(1), tag.getRelatedCommit());
	}
	
	@Test
	public void testSkipBuildsIfParentUnforked() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		
		// fork unTillDb
		IAction action = releaser.getProductionReleaseAction(UNTILLDB);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		checkUnTillDbForked();

		ReleaseBranch rbUBL = new ReleaseBranch(compUBL);
		ReleaseBranch rbUnTillDb = new ReleaseBranch(compUnTillDb);
		ReleaseBranch rbUnTill = new ReleaseBranch(compUnTill);

		assertFalse(env.getUnTillVCS().getBranches("").contains(rbUnTill.getName()));
		assertTrue(env.getUnTillDbVCS().getBranches("").contains(rbUnTillDb.getName()));
		assertFalse(env.getUblVCS().getBranches("").contains(rbUBL.getName()));
		
		// fork unTill. unTillDb build must be skipped
		// simulate UBL and unTill BRANCHED dev branch status
		env.generateContent(env.getUblVCS(), compUBL.getVcsRepository().getDevBranch(), "test file", "test content", LogTag.SCM_VER);
		env.generateContent(env.getUnTillVCS(), compUnTill.getVcsRepository().getDevBranch(), "test file", "test content", LogTag.SCM_VER);
		action = releaser.getProductionReleaseAction(UNTILL);
		Expectations exp = new Expectations();
		exp.put(UNTILLDB, ActionNone.class);
		exp.put(UNTILL, SCMActionFork.class);
		exp.put(UNTILL, "fromstatus", ReleaseBranchStatus.MISSING);
		exp.put(UNTILL, "tostatus", ReleaseBranchStatus.MDEPS_ACTUAL);
		exp.put(UBL, SCMActionFork.class);
		exp.put(UBL, "fromstatus", ReleaseBranchStatus.MISSING);
		exp.put(UBL, "tostatus", ReleaseBranchStatus.MDEPS_ACTUAL);
		checkChildActionsTypes(action, exp);
		try (IProgress progress = new ProgressConsole(action.toString(), ">>> ", "<<< ")) {
			action.execute(progress);
		}
		checkUnTillForked();
	}
	
	@Test
	public void testBuildPatchOnPreviousRelease() throws Exception {
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		SCMReleaser releaser = new SCMReleaser();
		
		// fork unTillDb 2.59
		IAction action = releaser.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// build unTillDb 2.59.1
		action = releaser.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// fork new unTillDb Release 2.60
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevBranch(), "feature added");
		action = releaser.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		// build new unTillDbRelease 2.60.1
		action = releaser.getProductionReleaseAction(UNTILLDB);
		action.execute(new NullProgress());
		
		assertEquals(env.getUnTillDbVer().toNextMinor().toRelease(), new ReleaseBranch(compUnTillDb).getVersion());
		
		//check desired release version is selected
		Component comp = new Component(UNTILLDB + ":2.59.1");
		ReleaseBranch rb = new ReleaseBranch(comp);
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toPreviousMinor().toRelease(), rb.getVersion());
		
		// add feature for 2.59.2
		env.generateFeatureCommit(env.getUnTillDbVCS(), rb.getName(), "2.59.2 feature added");
		
		// build new unTIllDb patch
		action = releaser.getProductionReleaseAction(comp);
		action.execute(new NullProgress());
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toPreviousMinor().toNextPatch().toRelease(), new ReleaseBranch(comp).getVersion());
	}
}