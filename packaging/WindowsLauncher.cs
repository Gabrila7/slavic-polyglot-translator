using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Windows.Forms;

[assembly: AssemblyTitle("SlavicPolyglot")]
[assembly: AssemblyDescription("Tradutor Poliglota EN/ES/RU com Teclado Virtual Cirilico")]
[assembly: AssemblyCompany("Gabriel Vinicius (@Gabrila7)")]
[assembly: AssemblyProduct("SlavicPolyglot Desktop")]
[assembly: AssemblyVersion("1.0.0.0")]

public class Program {
    [STAThread]
    public static void Main(string[] args) {
        string baseDir = AppDomain.CurrentDomain.BaseDirectory;
        string jarPath = Path.Combine(baseDir, "SlavicPolyglot.jar");
        if (!File.Exists(jarPath)) {
            jarPath = Path.Combine(baseDir, "dist", "SlavicPolyglot.jar");
        }

        if (!File.Exists(jarPath)) {
            MessageBox.Show(
                "O arquivo SlavicPolyglot.jar nao foi encontrado na pasta do aplicativo.",
                "SlavicPolyglot - Erro",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error
            );
            return;
        }

        string javaw = FindJavaExecutable();
        ProcessStartInfo psi = new ProcessStartInfo();
        psi.FileName = javaw;
        psi.Arguments = "-Dfile.encoding=UTF-8 -jar \"" + jarPath + "\"";
        psi.WorkingDirectory = baseDir;
        psi.UseShellExecute = false;
        psi.CreateNoWindow = true;

        try {
            Process.Start(psi);
        } catch (Exception ex) {
            MessageBox.Show(
                "Nao foi possivel iniciar o Java Runtime (JRE/JDK).\n" +
                "Verifique se o Java esta instalado na maquina.\n\nDetalhes: " + ex.Message,
                "SlavicPolyglot - Java nao encontrado",
                MessageBoxButtons.OK,
                MessageBoxIcon.Warning
            );
        }
    }

    private static string FindJavaExecutable() {
        string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
        if (!string.IsNullOrEmpty(javaHome)) {
            string candidate = Path.Combine(javaHome, "bin", "javaw.exe");
            if (File.Exists(candidate)) return candidate;
        }
        return "javaw.exe";
    }
}
