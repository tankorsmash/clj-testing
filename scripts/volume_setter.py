import argh

from pycaw.pycaw import AudioUtilities


def set_volume_to_pct(volume, vol_pct):
    if vol_pct > 1.0:
        print("Volume can't be higher than 1.0")
        return

    volume.SetMasterVolume(vol_pct, None)

def filter_sessions_by_procname(procname):
    sessions = AudioUtilities.GetAllSessions()
    matches = list(filter(lambda s: s.Process and s.Process.name() == procname,
                          sessions))
    # print(f"num matches {len(matches)} out of {len(sessions)} for '{procname}'")

    return matches

def set_volume_for_procs(procname="Discord.exe", vol_pct=1.0):
    for session in filter_sessions_by_procname("Discord.exe"):
        volume = session.SimpleAudioVolume
        set_volume_to_pct(volume, vol_pct)

    return f"Volume set to {vol_pct}"


if __name__ == "__main__":
    argh.dispatch_command(set_volume_for_procs)
