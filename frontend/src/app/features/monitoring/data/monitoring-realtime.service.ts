import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { MonitoringProblem } from '../../../core/models/monitoring-problem.model';
import { StompClientService } from '../../../core/realtime/stomp-client.service';
import { SourceAvailability } from '../../../core/models/source-availability.model';
import { ZabbixMetric } from '../../../core/models/zabbix-metric.model';
import { ZabbixProblem } from '../../../core/models/zabbix-problem.model';

@Injectable({ providedIn: 'root' })
export class MonitoringRealtimeService {
  constructor(private readonly stomp: StompClientService) {}

  problems$(): Observable<ZabbixProblem[]> {
    return this.stomp.subscribe<ZabbixProblem[]>('/topic/zabbix/problems');
  }

  metrics$(): Observable<ZabbixMetric[]> {
    return this.stomp.subscribe<ZabbixMetric[]>('/topic/zabbix/metrics');
  }

  monitoringProblems$(): Observable<MonitoringProblem[]> {
    return this.stomp.subscribe<MonitoringProblem[]>('/topic/monitoring/problems');
  }

  sourceAvailability$(): Observable<SourceAvailability> {
    return this.stomp.subscribe<SourceAvailability>('/topic/monitoring/sources');
  }
}
