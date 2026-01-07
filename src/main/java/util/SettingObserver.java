package util;
/**
 * Observer interface for reacting to global setting changes in SysData.
 */
public interface SettingObserver {
	/**
     * Called whenever a setting in SysData changes.
     *
     * @param key       the setting name (e.g. "musicEnabled")
     * @param newValue the new value of the setting
     */
	
    void onSettingChanged(String key, Object newValue);
}
