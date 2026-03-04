import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-error-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './error-page.html',
})
export class ErrorPage implements OnInit {
  private route = inject(ActivatedRoute);

  // Defaulting to 404 state
  errorCode = signal('404');
  errorTitle = signal('Page Not Found');
  errorMessage = signal("Sorry, the page you are looking for doesn't exist or has been moved.");

  ngOnInit() {
    // Read the ?code= parameter from the URL
    this.route.queryParams.subscribe((params) => {
      const code = params['code'];

      if (code === '403') {
        this.errorCode.set('403');
        this.errorTitle.set('Access Denied');
        this.errorMessage.set('You do not have the necessary permissions to access this area.');
      }
    });
  }
}
