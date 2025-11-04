// IShizukuUserService.aidl
package xyz.cirno.unfuckzui;

interface IShizukuUserService {

    void destroy() = 16777114; // Destroy method defined by Shizuku server
    void killPackageProcess(in String[] packages) = 1;
}