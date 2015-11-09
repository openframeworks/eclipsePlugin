package cc.openframeworks.plugin;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	public PreferencePage(){
		super(GRID);
	}
	
	@Override
	public void init(IWorkbench arg0) {
	    setPreferenceStore(Activator.getDefault().getPreferenceStore());
	    setDescription("openFrameworks preferences");
	}

	@Override
	protected void createFieldEditors() {
	    addField(new DirectoryFieldEditor("OF_ROOT", "&openFrameworks path:",
	            getFieldEditorParent()));
	}

}
