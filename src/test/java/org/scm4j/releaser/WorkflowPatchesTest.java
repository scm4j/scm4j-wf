package org.scm4j.releaser;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.scm4j.releaser.actions.IAction;
import org.scm4j.releaser.branch.ReleaseBranchCurrent;
import org.scm4j.releaser.branch.ReleaseBranchFactory;
import org.scm4j.releaser.branch.ReleaseBranchPatch;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.releaser.exceptions.ENoReleaseBranchForPatch;
import org.scm4j.releaser.exceptions.ENoReleases;
import org.scm4j.releaser.exceptions.EReleaseMDepsNotLocked;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class WorkflowPatchesTest extends WorkflowTestBase {

	@Test
	public void testPatches() throws Exception {
		IAction action = getAndExecActionTreeBuild(compUnTill);
		assertActionDoesForkAndBuildAll(action);
		checkUnTillBuilt();

		// add feature to existing unTillDb release
		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(compUnTillDb);
		env.generateFeatureCommit(env.getUnTillDbVCS(), crb.getName(), "patch feature added");

		// build unTillDb patch
		Component compUnTillDbPatch = new Component(UNTILLDB + ":" + env.getUnTillDbVer().toRelease());
		action = getAndExecActionTreeBuild(compUnTillDbPatch);
		assertActionDoesBuildBuild(action, compUnTillDb);

		ReleaseBranchPatch rb = ReleaseBranchFactory.getReleaseBranchPatch(compUnTillDbPatch);
		assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch().toNextPatch(),
				rb.getVersion());
		ExtendedStatusBuilder builder = new ExtendedStatusBuilder();
		assertEquals(BuildStatus.DONE, builder.getAndCacheMinorStatus(compUnTillDbPatch).getStatus());

		// Existing unTill and UBL release branches should actualize its mdeps
		action = getAndExecActionTreeBuild(compUnTill.clone(env.getUnTillVer().toRelease()));
		assertActionDoesBuildBuild(action, compUBL, BuildStatus.ACTUALIZE_PATCHES);
		assertActionDoesBuildBuild(action, compUnTill, BuildStatus.BUILD_MDEPS);
		assertActionDoesNothing(action, compUnTillDb);

		// check unTill uses new untillDb and UBL versions in existing unTill release branch.
		rb = ReleaseBranchFactory.getReleaseBranchPatch(compUnTill.clone(env.getUnTillVer().toRelease()));
		
		List<Component> mdeps = rb.getMDeps();
		for (Component mdep : mdeps) {
			if (mdep.getName().equals(UBL)) {
				assertEquals(env.getUblVer().toReleaseZeroPatch().toNextPatch(), mdep.getVersion());
			} else if (mdep.getName().equals(UNTILLDB)) {
				assertEquals(env.getUnTillDbVer().toReleaseZeroPatch().toNextPatch(), mdep.getVersion());
			} else {
				fail();
			}
		}
	}

	@Test
	public void testBuildPatchOnExistingRelease() throws Exception {
		// fork unTillDb 2.59
		IAction action = getAndExecActionTreeBuild(compUnTillDb);
		assertActionDoesForkAndBuild(action, compUnTillDb);
		checkUnTillDbBuilt();

		// fork new unTillDb Release 2.60
		env.generateFeatureCommit(env.getUnTillDbVCS(), compUnTillDb.getVcsRepository().getDevelopBranch(), "feature added");
		action = getAndExecActionTreeBuild(compUnTillDb);
		assertActionDoesForkAndBuild(action, compUnTillDb);
		checkUnTillDbBuilt(2);

		ReleaseBranchCurrent crb = ReleaseBranchFactory.getCRB(compUnTillDb);
		assertEquals(env.getUnTillDbVer().toNextMinor().toRelease(), crb.getVersion());

		// add feature for 2.59.1
		Component compToPatch = new Component(UNTILLDB + ":2.59.1");
		ReleaseBranchPatch rb = ReleaseBranchFactory.getReleaseBranchPatch(compToPatch);
		env.generateFeatureCommit(env.getUnTillDbVCS(), rb.getName(), "2.59.1 feature merged");

		// build new unTillDb patch 2.59.1
		action = getAndExecActionTreeBuild(compToPatch);
		assertActionDoesBuildBuild(action, compUnTillDb);
		rb = ReleaseBranchFactory.getReleaseBranchPatch(compToPatch);
		assertEquals(dbUnTillDb.getVersion().toPreviousMinor().toPreviousMinor().toNextPatch().toRelease(), rb.getVersion());
	}
	
	@Test
	public void testExceptionOnPatchOnUnexistingBranch() {
		// try do build a patch for unreleased version
		Component compWithUnexistingVersion = new Component(UNTILLDB + ":2.70.0");
		try {
			getAndExecActionTreeBuild(compWithUnexistingVersion);
			fail();
		} catch (ENoReleaseBranchForPatch e) {
		}
	}

	@Test
	public void testExceptionOnPatchOnUnreleasedComponent() {
		// fork unTillDb
		IAction action = getAndExecActionTreeFork(compUnTillDb);
		assertActionDoesFork(action, compUnTillDb);
		checkUnTillDbForked();

		// try to build a patch on existing branch with no releases
		Component compUnTillDbVersioned = compUnTillDb.clone(env.getUnTillDbVer().toReleaseZeroPatch());
		try {
			getAndExecActionTreeBuild(compUnTillDbVersioned);
			fail();
		} catch(ENoReleases e) {
		}
	}

	@Test
	public void testExceptionMDepsNotLockedOnPatch() {
		// build unTillDb
		IAction action = getAndExecActionTreeBuild(compUnTillDb);
		assertActionDoesForkAndBuild(action, compUnTillDb);
		checkUnTillDbBuilt();

		// simulate not locked mdep
		Component nonLockedMDep = new Component("unexisting.com:unexisting");
		MDepsFile mdf = new MDepsFile(Arrays.asList(nonLockedMDep));
		ReleaseBranchCurrent rb = ReleaseBranchFactory.getCRB(compUnTillDb);
		env.getUnTillDbVCS().setFileContent(rb.getName(), Utils.MDEPS_FILE_NAME,
				mdf.toFileContent(), "mdeps file added");

		// try to build patch
		try {
			getAndExecActionTreeBuild(compUnTillDb.clone(rb.getVersion()));
			fail();
		} catch (EReleaseMDepsNotLocked e) {
			assertThat(e.getNonLockedMDeps(), Matchers.<Collection<Component>>allOf(
					hasSize(1),
					contains(nonLockedMDep)));
		}
	}
}
