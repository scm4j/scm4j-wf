package org.scm4j.releaser.branch;

import java.util.ArrayList;
import java.util.List;

import org.scm4j.commons.Version;
import org.scm4j.releaser.Utils;
import org.scm4j.releaser.conf.Component;
import org.scm4j.releaser.conf.MDepsFile;
import org.scm4j.vcs.api.IVCS;
import org.scm4j.vcs.api.exceptions.EVCSBranchNotFound;
import org.scm4j.vcs.api.exceptions.EVCSFileNotFound;

public final class ReleaseBranchFactory {
	
	public static ReleaseBranch getReleaseBranchPatch(Component comp) {
		IVCS vcs = comp.getVCS();
		String name = Utils.getReleaseBranchName(comp, comp.getVersion());
		boolean exists;
		Version version;
		List<Component> mdeps;
		try {
			version = new Version(vcs.getFileContent(name, Utils.VER_FILE_NAME, null)).toRelease();
			exists = true;
			mdeps = getMDepsRelease(comp, name);
		} catch (EVCSBranchNotFound e) {
			exists = false;
			version = null;
			mdeps = new ArrayList<>(); // will not be used because ENoReleaseBranchForPatch will be thrown 
		}
		
		return new ReleaseBranch(mdeps, exists, name, version, comp, null); 
	}
	
	public static ReleaseBranch getCRB(Component comp) {
		IVCS vcs = comp.getVCS();
		Version devVersion = Utils.getDevVersion(comp);
		Version version;
		boolean exists;
		String name = Utils.getReleaseBranchName(comp, devVersion.toPreviousMinor());
		try {
			version = new Version(vcs.getFileContent(name, Utils.VER_FILE_NAME, null)).toRelease();
			exists = true;
		} catch (EVCSBranchNotFound e) {
			version = devVersion.toReleaseZeroPatch();
			exists = false;
		}
		List<Component> mdeps = exists && version.getPatch().equals(Utils.ZERO_PATCH) ? getMDepsRelease(comp, name) : getMDepsDevelop(comp);
	
		return new ReleaseBranch(mdeps, exists, name, version, comp, devVersion);
	}
	
	public static List<Component> getMDepsRelease(Component comp, String releaseBranchName) {
		try {
			String mDepsFileContent = comp.getVCS().getFileContent(releaseBranchName, Utils.MDEPS_FILE_NAME, null);
			return new MDepsFile(mDepsFileContent).getMDeps();
		} catch (EVCSFileNotFound e) {
			return new ArrayList<>();
		}
	}
	
	public static List<Component> getMDepsDevelop(Component comp) {
		List<Component> res = new ArrayList<>();
		for (Component mDep : getMDepsRelease(comp, null)) {
			res.add(mDep.clone(""));
		}
		return res;
	}
	
	private ReleaseBranchFactory() {
		
	}
}