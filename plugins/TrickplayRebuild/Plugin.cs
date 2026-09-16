using MediaBrowser.Common.Configuration;
using MediaBrowser.Common.Plugins;
using MediaBrowser.Controller;
using MediaBrowser.Controller.Plugins;
using MediaBrowser.Model.Plugins;
using MediaBrowser.Model.Serialization;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

namespace Findroid.TrickplayRebuild;

public sealed class Plugin(IApplicationPaths paths, IXmlSerializer serializer)
    : BasePlugin<BasePluginConfiguration>(paths, serializer)
{
    public override string Name => "Findroid Trickplay Rebuild";
    public override string Description => "Rebuild a single video's trickplay without refreshing metadata.";
    public override Guid Id => Guid.Parse("64bed64c-dc31-424e-b462-d79e6eeff504");
}

public sealed class ServiceRegistrator : IPluginServiceRegistrator
{
    public void RegisterServices(IServiceCollection services, IServerApplicationHost host)
    {
        // No replacement/decorator of any Jellyfin service or scheduler.
        services.AddSingleton(TimeProvider.System);
        services.AddSingleton<IRebuildExecutor, JellyfinRebuildExecutor>();
        services.AddSingleton<RebuildQueue>();
        services.AddSingleton<IHostedService>(provider => provider.GetRequiredService<RebuildQueue>());
    }
}
