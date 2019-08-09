import org.gradle.api.Plugin
import org.gradle.api.Project

 class MyPlugin implements Plugin<Project> {

//    void apply(Project project) {
//        System.out.println("========================");
//        System.out.println("hello gradle plugin!");
//        System.out.println("========================");
//    }
    @Override
    void apply(Project project) {

        project.extensions.create('apkdistconf', ApkDistExtension);

        project.afterEvaluate {

            //只可以在 android application 或者 android lib 项目中使用
            if (!project.android) {
                throw new IllegalStateException('Must apply \'com.android.application\' or \'com.android.library\' first!')
            }

            //配置不能为空
            if (project.apkdistconf.nameMap == null || project.apkdistconf.destDir == null) {
                project.logger.info('Apkdist conf should be set!')
                return
            }

            Closure nameMap = project['apkdistconf'].nameMap
            String destDir = project['apkdistconf'].destDir

            //枚举每一个 build variant
            project.android.applicationVariants.all { variant ->
                variant.outputs.each { output ->
                    File file = output.outputFile
                    output.outputFile = new File(destDir, nameMap(file.getName()))
                }
            }
        }
    }
}