package cc.openframeworks.plugin.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.internal.core.settings.model.CProjectDescriptionManager;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedProject;
import org.eclipse.cdt.managedbuilder.internal.core.ToolChain;
import org.eclipse.cdt.managedbuilder.language.settings.providers.GCCBuiltinSpecsDetector;
import org.eclipse.core.resources.*;

import java.io.*;
import org.eclipse.ui.*;
import org.osgi.framework.Bundle;

import cc.openframeworks.plugin.Activator;
import cc.openframeworks.plugin.Utils;


public class OFNewWizard extends Wizard implements INewWizard {
	private OFNewWizardPage page1;
	private SelectAddonPage pageAddons;
	private ISelection selection;
	
	/**
	 * Constructor for OFNewWizard.
	 */
	public OFNewWizard() {
		super();
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		pageAddons = new SelectAddonPage();
		page1 = new OFNewWizardPage(selection,pageAddons);
		addPage(page1);
		addPage(pageAddons);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		System.out.println("perform finish");
		final File folderName = page1.getProjectFolder();
		final String projectName = page1.getProjectName();
		final String[] addons = pageAddons.getSelectedAddons();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					System.out.println("do finish");
					doFinish(folderName, projectName, addons, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			String msg = realException.toString() + "\n";
			for(StackTraceElement element: realException.getStackTrace()){
				msg += "\n" + element.toString();
			}
			MessageDialog.openError(getShell(), "Error",  msg);
			return false;
		}
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 * @throws Exception 
	 */

	private void doFinish(
		File projectFolder,
		String projectName,
		String[] addons,
		IProgressMonitor monitor)
		throws CoreException {
		// create a sample file
		monitor.beginTask("Creating " + projectName, 100);
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		System.out.println(root.getLocation());
		IProject newProjectHandle = root.getProject(projectName);
		
		for(String p: ManagedBuildManager.getExtensionProjectTypeMap().keySet()){
			System.out.println(p);
		}
		IProjectType projType = ManagedBuildManager.getProjectType("cdt.managedbuild.target.gnu.exe");
		if(projType==null){
			Utils.throwCoreException("error getting exe project type");
		}
		//Assert.assertNotNull(newProjectHandle);
		//Assert.assertFalse(newProjectHandle.exists());

		IProjectDescription description = workspace.newProjectDescription(newProjectHandle.getName());
		if(projectFolder.getName().equals(projectName)){
			description.setLocationURI(projectFolder.toURI());
		}else{
			File path = new File(projectFolder.getAbsolutePath()+"/"+projectName);
			description.setLocationURI(path.toURI());
		}
		IProject project;
		try {
			project = CCorePlugin.getDefault().createCDTProject(description, newProjectHandle, monitor);
		
			//Assert.assertTrue(newProjectHandle.isOpen());
	
			ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
			ICProjectDescription des = mngr.createProjectDescription(project,false);
			
			
			
			
			IToolChain[] tcs = ManagedBuildManager.getRealToolChains();
			IToolChain tc=null;
			for (int i=0; i<tcs.length; i++) {
				System.out.println(tcs[i].getName());
				if(tcs[i].getName().contains("Linux GCC")) {
					tc = tcs[i];
					break;
				}
			}
			
			//IManagedProject mProj = ManagedBuildManager.createManagedProject(project, projType);
			String ofRoot = Activator.getDefault().getPreferenceStore().getString("OF_ROOT");

			IManagedProject mProj = new  ManagedProject(des);
			IManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);
			info.setManagedProject(mProj);
			
			// Create Debug config
			IConfiguration cfgDebug = new Configuration((ManagedProject) mProj, (ToolChain)tc,"cc.openframeworks.configurations.debug", "Debug");
			CCProjectNature.addCCNature(project, new NullProgressMonitor());
			
			IBuilder bld = cfgDebug.getEditableBuilder();
			bld.setParallelBuildOn(true);
			bld.setManagedBuildOn(false);
			bld.setCommand("make");
			if(ofRoot!=null && !ofRoot.equals("")){
				bld.setIncrementalBuildTarget("Debug OF_ROOT="+ofRoot);
			}else{
				bld.setIncrementalBuildTarget("Debug");
			}
			bld.setCleanBuildTarget("CleanDebug");
			
			CConfigurationData data = cfgDebug.getConfigurationData();
			des.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);						
			
			// Create Release config
			IConfiguration cfgRelease = new Configuration((ManagedProject) mProj, (ToolChain)tc,"cc.openframeworks.configurations.release", "Release");

			bld = cfgRelease.getEditableBuilder();
			bld.setParallelBuildOn(true);
			bld.setManagedBuildOn(false);
			bld.setCommand("make");
			if(ofRoot!=null && !ofRoot.equals("")){
				bld.setIncrementalBuildTarget("Release OF_ROOT="+ofRoot);
			}else{
				bld.setIncrementalBuildTarget("Release");
			}
			bld.setCleanBuildTarget("CleanRelease");
			
			data = cfgRelease.getConfigurationData();
			des.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
			
			// Persist the project description
			des.setCdtProjectCreated();
			mngr.setProjectDescription(project, des);
			
			
			// set gcc builtins parser to c++14
			ICProjectDescription prjDescriptionWritable = CProjectDescriptionManager.getInstance().getProjectDescription(project, true);
			ICConfigurationDescription[] cfgDescriptions = prjDescriptionWritable.getConfigurations();
			for(ICConfigurationDescription cfgDescriptionWritable: cfgDescriptions){
				List<ILanguageSettingsProvider> storedProviders = ((ILanguageSettingsProvidersKeeper) cfgDescriptionWritable).getLanguageSettingProviders();
				List<ILanguageSettingsProvider> newproviders = new ArrayList<ILanguageSettingsProvider>();
				String[] settingsIds = ((ILanguageSettingsProvidersKeeper)cfgDescriptionWritable).getDefaultLanguageSettingsProvidersIds();
				for(ILanguageSettingsProvider provider: storedProviders){
					if(provider.getId().equals("org.eclipse.cdt.managedbuilder.core.GCCBuiltinSpecsDetector")){
						GCCBuiltinSpecsDetector gccbuiltin = (GCCBuiltinSpecsDetector)LanguageSettingsManager.getExtensionProviderCopy("org.eclipse.cdt.managedbuilder.core.GCCBuiltinSpecsDetector", true);
						gccbuiltin.setCommand("${COMMAND} ${FLAGS} -E -P -v -dD \"${INPUTS}\" -std=c++1y");
						LanguageSettingsManager.setStoringEntriesInProjectArea(gccbuiltin, true);
						newproviders.add(gccbuiltin);
					}else{
						newproviders.add(provider);
					}
				}
				((ILanguageSettingsProvidersKeeper)cfgDescriptionWritable).setLanguageSettingProviders(newproviders);
				((ILanguageSettingsProvidersKeeper)cfgDescriptionWritable).setDefaultLanguageSettingsProvidersIds(settingsIds);
				mngr.setProjectDescription(project, prjDescriptionWritable);
			}

	
			// Copy files from template
			Bundle bundle = Platform.getBundle("cc.openframeworks.plugin");
			URL fileURL = bundle.getEntry("templates/ofApp");
			File file = null;
			try {
			    file = new File(FileLocator.resolve(fileURL).toURI());
			} catch (URISyntaxException e1) {
			    e1.printStackTrace();
			} catch (IOException e1) {
			    e1.printStackTrace();
			}
			if(file!=null){
				copyRecursive(file,project.getLocation().toFile(),monitor);
			}
			project.refreshLocal(0, null);
			
			
			// Parse addons
			Utils.reparseAddons(project, addons, ofRoot);
			
			project.refreshLocal(IResource.DEPTH_ONE, monitor);
			project.setPersistentProperty(
					new QualifiedName("", "OF_ROOT"),
					ofRoot);
			
			project.build(project.getActiveBuildConfig(),0,monitor);
			monitor.done();
		
		} catch (OperationCanceledException e) {
			Utils.throwCoreException(e.getMessage());
		}
	}

	private void copyRecursive(File src, File dst, IProgressMonitor monitor) {
		for(File f: src.listFiles()){
			File dstPath = new File(dst.getAbsolutePath()+"/"+f.getName());
			try {
				Files.copy(f.toPath(),dstPath.toPath());
				monitor.worked(1);
			} catch (IOException e) {
				//e.printStackTrace();
			}
			if(f.isDirectory()){
				copyRecursive(f, dstPath, monitor);
			}
		}
		
	}

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}